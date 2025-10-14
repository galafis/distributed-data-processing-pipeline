"""
Database utilities for data pipeline.

Provides helper functions for working with databases.

Author: Gabriel Demetrios Lafis
"""

import logging
from typing import Optional, Dict, Any, List
from contextlib import contextmanager

logger = logging.getLogger(__name__)


class DatabaseUtils:
    """Utilities for working with databases."""
    
    def __init__(
        self,
        host: str,
        port: int,
        database: str,
        user: str,
        password: str,
        db_type: str = 'postgresql'
    ):
        """
        Initialize database utilities.
        
        Parameters
        ----------
        host : str
            Database host
        port : int
            Database port
        database : str
            Database name
        user : str
            Database user
        password : str
            Database password
        db_type : str
            Database type (postgresql, mysql, etc.)
        """
        self.host = host
        self.port = port
        self.database = database
        self.user = user
        self.password = password
        self.db_type = db_type
        self._connection = None
    
    @property
    def connection_string(self) -> str:
        """Get database connection string."""
        if self.db_type == 'postgresql':
            return f"postgresql://{self.user}:{self.password}@{self.host}:{self.port}/{self.database}"
        elif self.db_type == 'mysql':
            return f"mysql+pymysql://{self.user}:{self.password}@{self.host}:{self.port}/{self.database}"
        else:
            raise ValueError(f"Unsupported database type: {self.db_type}")
    
    def get_connection(self):
        """Get database connection."""
        if self._connection is None:
            try:
                import sqlalchemy
                engine = sqlalchemy.create_engine(self.connection_string)
                self._connection = engine.connect()
            except ImportError:
                logger.error("sqlalchemy not installed. Install with: pip install sqlalchemy")
                raise
        return self._connection
    
    @contextmanager
    def connection(self):
        """
        Context manager for database connection.
        
        Yields
        ------
        connection
            Database connection
        """
        conn = self.get_connection()
        try:
            yield conn
        finally:
            if conn:
                conn.close()
    
    def execute_query(
        self,
        query: str,
        params: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """
        Execute SQL query and return results.
        
        Parameters
        ----------
        query : str
            SQL query
        params : dict, optional
            Query parameters
            
        Returns
        -------
        list
            List of result rows as dictionaries
        """
        logger.info(f"Executing query: {query[:100]}...")
        
        with self.connection() as conn:
            if params:
                result = conn.execute(query, params)
            else:
                result = conn.execute(query)
            
            # Convert to list of dicts
            columns = result.keys()
            rows = [dict(zip(columns, row)) for row in result.fetchall()]
            
            logger.info(f"Query returned {len(rows)} rows")
            return rows
    
    def execute_update(
        self,
        query: str,
        params: Optional[Dict[str, Any]] = None
    ) -> int:
        """
        Execute update/insert/delete query.
        
        Parameters
        ----------
        query : str
            SQL query
        params : dict, optional
            Query parameters
            
        Returns
        -------
        int
            Number of rows affected
        """
        logger.info(f"Executing update: {query[:100]}...")
        
        with self.connection() as conn:
            if params:
                result = conn.execute(query, params)
            else:
                result = conn.execute(query)
            
            rows_affected = result.rowcount
            logger.info(f"Updated {rows_affected} rows")
            return rows_affected
    
    def read_table(
        self,
        table_name: str,
        columns: Optional[List[str]] = None,
        where_clause: Optional[str] = None,
        limit: Optional[int] = None
    ) -> List[Dict[str, Any]]:
        """
        Read data from a table.
        
        Parameters
        ----------
        table_name : str
            Table name
        columns : list, optional
            Columns to select (default: all)
        where_clause : str, optional
            WHERE clause (without WHERE keyword)
        limit : int, optional
            Maximum number of rows to return
            
        Returns
        -------
        list
            List of rows as dictionaries
        """
        cols = ','.join(columns) if columns else '*'
        query = f"SELECT {cols} FROM {table_name}"
        
        if where_clause:
            query += f" WHERE {where_clause}"
        
        if limit:
            query += f" LIMIT {limit}"
        
        return self.execute_query(query)
    
    def write_table(
        self,
        table_name: str,
        data: List[Dict[str, Any]],
        if_exists: str = 'append'
    ) -> int:
        """
        Write data to a table using pandas.
        
        Parameters
        ----------
        table_name : str
            Table name
        data : list
            List of dictionaries to write
        if_exists : str
            How to behave if table exists ('fail', 'replace', 'append')
            
        Returns
        -------
        int
            Number of rows written
        """
        try:
            import pandas as pd
            import sqlalchemy
            
            df = pd.DataFrame(data)
            engine = sqlalchemy.create_engine(self.connection_string)
            
            rows_written = df.to_sql(
                table_name,
                engine,
                if_exists=if_exists,
                index=False
            )
            
            logger.info(f"Wrote {rows_written} rows to {table_name}")
            return rows_written
            
        except ImportError:
            logger.error("pandas not installed. Install with: pip install pandas")
            raise
    
    def table_exists(self, table_name: str) -> bool:
        """
        Check if table exists.
        
        Parameters
        ----------
        table_name : str
            Table name
            
        Returns
        -------
        bool
            True if table exists
        """
        try:
            query = f"SELECT 1 FROM {table_name} LIMIT 1"
            self.execute_query(query)
            return True
        except:
            return False
    
    def get_table_row_count(self, table_name: str) -> int:
        """
        Get number of rows in table.
        
        Parameters
        ----------
        table_name : str
            Table name
            
        Returns
        -------
        int
            Number of rows
        """
        query = f"SELECT COUNT(*) as count FROM {table_name}"
        result = self.execute_query(query)
        return result[0]['count']
    
    def close(self):
        """Close database connection."""
        if self._connection:
            self._connection.close()
            self._connection = None


def create_db_utils_from_config(config: Dict[str, Any]) -> DatabaseUtils:
    """
    Create DatabaseUtils instance from configuration.
    
    Parameters
    ----------
    config : dict
        Configuration dictionary with database settings
        
    Returns
    -------
    DatabaseUtils
        Configured database utilities instance
    """
    return DatabaseUtils(
        host=config.get('host', 'localhost'),
        port=config.get('port', 5432),
        database=config.get('database', 'pipeline'),
        user=config.get('user', 'postgres'),
        password=config.get('password', ''),
        db_type=config.get('type', 'postgresql')
    )
