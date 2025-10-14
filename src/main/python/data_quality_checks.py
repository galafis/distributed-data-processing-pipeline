"""
Data quality checks implementation.

Provides functions for validating data quality.

Author: Gabriel Demetrios Lafis
"""

import logging
from typing import Dict, List, Optional, Callable
from dataclasses import dataclass

logger = logging.getLogger(__name__)


@dataclass
class QualityCheckResult:
    """Result of a quality check."""
    check_name: str
    passed: bool
    message: str
    metric_value: Optional[float] = None
    threshold: Optional[float] = None


class DataQualityChecker:
    """Data quality checker for PySpark DataFrames."""
    
    def __init__(self, threshold_config: Optional[Dict[str, float]] = None):
        """
        Initialize data quality checker.
        
        Parameters
        ----------
        threshold_config : dict, optional
            Configuration for quality thresholds
        """
        self.threshold_config = threshold_config or {
            'null_threshold': 0.05,  # 5%
            'duplicate_threshold': 0.01,  # 1%
        }
        self.results = []
    
    def check_null_percentage(
        self,
        df,
        columns: Optional[List[str]] = None,
        threshold: Optional[float] = None
    ) -> List[QualityCheckResult]:
        """
        Check null percentage in columns.
        
        Parameters
        ----------
        df : DataFrame
            Spark DataFrame to check
        columns : list, optional
            Columns to check (default: all)
        threshold : float, optional
            Maximum acceptable null percentage
            
        Returns
        -------
        list
            List of quality check results
        """
        threshold = threshold or self.threshold_config['null_threshold']
        columns = columns or df.columns
        
        results = []
        total_count = df.count()
        
        for col in columns:
            null_count = df.filter(df[col].isNull()).count()
            null_pct = null_count / total_count if total_count > 0 else 0
            
            passed = null_pct <= threshold
            result = QualityCheckResult(
                check_name=f"null_check_{col}",
                passed=passed,
                message=f"Column '{col}' null percentage: {null_pct:.2%} (threshold: {threshold:.2%})",
                metric_value=null_pct,
                threshold=threshold
            )
            
            results.append(result)
            self.results.append(result)
            
            if not passed:
                logger.warning(result.message)
        
        return results
    
    def check_completeness(
        self,
        df,
        required_columns: List[str]
    ) -> QualityCheckResult:
        """
        Check if all required columns are present.
        
        Parameters
        ----------
        df : DataFrame
            Spark DataFrame to check
        required_columns : list
            List of required column names
            
        Returns
        -------
        QualityCheckResult
            Quality check result
        """
        actual_columns = set(df.columns)
        missing_columns = set(required_columns) - actual_columns
        
        passed = len(missing_columns) == 0
        message = (
            "All required columns present" if passed
            else f"Missing columns: {', '.join(missing_columns)}"
        )
        
        result = QualityCheckResult(
            check_name="completeness_check",
            passed=passed,
            message=message
        )
        
        self.results.append(result)
        if not passed:
            logger.warning(message)
        
        return result
    
    def check_duplicate_percentage(
        self,
        df,
        subset: Optional[List[str]] = None,
        threshold: Optional[float] = None
    ) -> QualityCheckResult:
        """
        Check duplicate percentage.
        
        Parameters
        ----------
        df : DataFrame
            Spark DataFrame to check
        subset : list, optional
            Columns to check for duplicates (default: all)
        threshold : float, optional
            Maximum acceptable duplicate percentage
            
        Returns
        -------
        QualityCheckResult
            Quality check result
        """
        threshold = threshold or self.threshold_config['duplicate_threshold']
        
        total_count = df.count()
        if subset:
            distinct_count = df.select(subset).distinct().count()
        else:
            distinct_count = df.distinct().count()
        
        duplicate_count = total_count - distinct_count
        duplicate_pct = duplicate_count / total_count if total_count > 0 else 0
        
        passed = duplicate_pct <= threshold
        message = f"Duplicate percentage: {duplicate_pct:.2%} (threshold: {threshold:.2%})"
        
        result = QualityCheckResult(
            check_name="duplicate_check",
            passed=passed,
            message=message,
            metric_value=duplicate_pct,
            threshold=threshold
        )
        
        self.results.append(result)
        if not passed:
            logger.warning(message)
        
        return result
    
    def check_record_count(
        self,
        df,
        min_records: int = 1
    ) -> QualityCheckResult:
        """
        Check minimum record count.
        
        Parameters
        ----------
        df : DataFrame
            Spark DataFrame to check
        min_records : int
            Minimum acceptable record count
            
        Returns
        -------
        QualityCheckResult
            Quality check result
        """
        count = df.count()
        passed = count >= min_records
        message = f"Record count: {count} (minimum: {min_records})"
        
        result = QualityCheckResult(
            check_name="record_count_check",
            passed=passed,
            message=message,
            metric_value=float(count),
            threshold=float(min_records)
        )
        
        self.results.append(result)
        if not passed:
            logger.warning(message)
        
        return result
    
    def check_value_range(
        self,
        df,
        column: str,
        min_value: Optional[float] = None,
        max_value: Optional[float] = None
    ) -> QualityCheckResult:
        """
        Check if values are within expected range.
        
        Parameters
        ----------
        df : DataFrame
            Spark DataFrame to check
        column : str
            Column to check
        min_value : float, optional
            Minimum acceptable value
        max_value : float, optional
            Maximum acceptable value
            
        Returns
        -------
        QualityCheckResult
            Quality check result
        """
        from pyspark.sql.functions import col
        
        filtered_df = df
        if min_value is not None:
            filtered_df = filtered_df.filter(col(column) >= min_value)
        if max_value is not None:
            filtered_df = filtered_df.filter(col(column) <= max_value)
        
        total_count = df.count()
        valid_count = filtered_df.count()
        invalid_count = total_count - valid_count
        
        passed = invalid_count == 0
        message = (
            f"All values in column '{column}' within range "
            f"[{min_value}, {max_value}]" if passed
            else f"Found {invalid_count} values outside range in column '{column}'"
        )
        
        result = QualityCheckResult(
            check_name=f"range_check_{column}",
            passed=passed,
            message=message,
            metric_value=float(invalid_count)
        )
        
        self.results.append(result)
        if not passed:
            logger.warning(message)
        
        return result
    
    def run_custom_check(
        self,
        check_name: str,
        check_function: Callable,
        **kwargs
    ) -> QualityCheckResult:
        """
        Run a custom quality check.
        
        Parameters
        ----------
        check_name : str
            Name of the check
        check_function : callable
            Function that returns (passed: bool, message: str)
        **kwargs
            Arguments to pass to check_function
            
        Returns
        -------
        QualityCheckResult
            Quality check result
        """
        try:
            passed, message = check_function(**kwargs)
            result = QualityCheckResult(
                check_name=check_name,
                passed=passed,
                message=message
            )
        except Exception as e:
            result = QualityCheckResult(
                check_name=check_name,
                passed=False,
                message=f"Check failed with error: {str(e)}"
            )
            logger.error(f"Custom check '{check_name}' failed: {e}")
        
        self.results.append(result)
        return result
    
    def get_summary(self) -> Dict[str, any]:
        """
        Get summary of all quality checks.
        
        Returns
        -------
        dict
            Summary of quality check results
        """
        total_checks = len(self.results)
        passed_checks = sum(1 for r in self.results if r.passed)
        failed_checks = total_checks - passed_checks
        
        return {
            'total_checks': total_checks,
            'passed_checks': passed_checks,
            'failed_checks': failed_checks,
            'success_rate': passed_checks / total_checks if total_checks > 0 else 0,
            'all_passed': failed_checks == 0,
            'results': [
                {
                    'check_name': r.check_name,
                    'passed': r.passed,
                    'message': r.message,
                    'metric_value': r.metric_value,
                    'threshold': r.threshold
                }
                for r in self.results
            ]
        }
    
    def clear_results(self):
        """Clear all stored results."""
        self.results = []


def run_data_quality_checks(
    input_path: str,
    date: str,
    checks: List[str]
) -> bool:
    """
    Run data quality checks on input data.
    
    Parameters
    ----------
    input_path : str
        Path to input data
    date : str
        Date for processing
    checks : list
        List of check names to run
        
    Returns
    -------
    bool
        True if all checks passed
    """
    from pyspark.sql import SparkSession
    
    spark = SparkSession.builder \
        .appName("DataQualityChecks") \
        .config("spark.ui.enabled", "false") \
        .getOrCreate()
    
    try:
        df = spark.read.parquet(input_path)
        checker = DataQualityChecker()
        
        if 'schema_validation' in checks:
            checker.check_completeness(df, ['transactionId', 'customerId', 'amount'])
        
        if 'completeness' in checks:
            checker.check_null_percentage(df)
        
        if 'freshness' in checks:
            checker.check_record_count(df, min_records=1)
        
        if 'row_count' in checks:
            checker.check_record_count(df, min_records=1)
        
        if 'null_check' in checks:
            checker.check_null_percentage(df)
        
        if 'duplicate_check' in checks:
            checker.check_duplicate_percentage(df)
        
        summary = checker.get_summary()
        logger.info(f"Quality checks summary: {summary}")
        
        return summary['all_passed']
        
    finally:
        spark.stop()
