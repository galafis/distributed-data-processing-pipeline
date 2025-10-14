"""
Unit tests for Airflow DAG.

Author: Gabriel Demetrios Lafis
"""

import os
import sys
import pytest
from unittest.mock import Mock, patch, MagicMock
from datetime import datetime

# Add dags to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../../dags'))

from data_pipeline_dag import check_data_availability, validate_output, send_completion_notification


class TestDataPipelineDAG:
    """Test suite for Data Pipeline DAG functions."""
    
    def test_check_data_availability_success(self, tmp_path):
        """Test successful data availability check."""
        # Create test directory with parquet files
        test_path = tmp_path / "test_data"
        test_path.mkdir()
        (test_path / "data1.parquet").touch()
        (test_path / "data2.parquet").touch()
        
        context = {
            'params': {
                'input_path': str(test_path)
            }
        }
        
        result = check_data_availability(**context)
        assert result == 2
    
    def test_check_data_availability_path_not_found(self):
        """Test data availability check when path doesn't exist."""
        context = {
            'params': {
                'input_path': '/nonexistent/path'
            }
        }
        
        with pytest.raises(FileNotFoundError):
            check_data_availability(**context)
    
    def test_check_data_availability_no_files(self, tmp_path):
        """Test data availability check when no parquet files found."""
        test_path = tmp_path / "empty_data"
        test_path.mkdir()
        
        context = {
            'params': {
                'input_path': str(test_path)
            }
        }
        
        with pytest.raises(ValueError, match="No parquet files found"):
            check_data_availability(**context)
    
    @patch('data_pipeline_dag.SparkSession')
    def test_validate_output_success(self, mock_spark_session):
        """Test successful output validation."""
        # Mock Spark DataFrame
        mock_df = MagicMock()
        mock_df.count.return_value = 100
        mock_df.filter.return_value.count.return_value = 0
        mock_df.customerId = MagicMock()
        
        # Mock Spark session
        mock_spark = MagicMock()
        mock_spark.read.format.return_value.load.return_value = mock_df
        mock_spark_session.builder.appName.return_value.getOrCreate.return_value = mock_spark
        
        context = {
            'params': {
                'output_path': '/test/output'
            }
        }
        
        # Should not raise any exception
        validate_output(**context)
        
        # Verify Spark session was stopped
        mock_spark.stop.assert_called_once()
    
    @patch('data_pipeline_dag.SparkSession')
    def test_validate_output_no_records(self, mock_spark_session):
        """Test output validation with no records."""
        # Mock Spark DataFrame with zero records
        mock_df = MagicMock()
        mock_df.count.return_value = 0
        
        # Mock Spark session
        mock_spark = MagicMock()
        mock_spark.read.format.return_value.load.return_value = mock_df
        mock_spark_session.builder.appName.return_value.getOrCreate.return_value = mock_spark
        
        context = {
            'params': {
                'output_path': '/test/output'
            }
        }
        
        with pytest.raises(AssertionError, match="No records in output"):
            validate_output(**context)
        
        # Verify Spark session was stopped even on error
        mock_spark.stop.assert_called_once()
    
    @patch('data_pipeline_dag.SparkSession')
    def test_validate_output_null_customers(self, mock_spark_session):
        """Test output validation with null customer IDs."""
        # Mock Spark DataFrame with null customers
        mock_df = MagicMock()
        mock_df.count.return_value = 100
        mock_filter = MagicMock()
        mock_filter.count.return_value = 5  # 5 null customers
        mock_df.filter.return_value = mock_filter
        mock_df.customerId = MagicMock()
        
        # Mock Spark session
        mock_spark = MagicMock()
        mock_spark.read.format.return_value.load.return_value = mock_df
        mock_spark_session.builder.appName.return_value.getOrCreate.return_value = mock_spark
        
        context = {
            'params': {
                'output_path': '/test/output'
            }
        }
        
        with pytest.raises(AssertionError, match="Found null customer IDs"):
            validate_output(**context)
        
        mock_spark.stop.assert_called_once()
    
    def test_send_completion_notification(self):
        """Test sending completion notification."""
        context = {
            'execution_date': datetime(2024, 1, 1),
            'task_instance': MagicMock()
        }
        
        result = send_completion_notification(**context)
        
        assert result is not None
        assert 'Pipeline Execution Completed' in result
        assert '2024-01-01' in result
        assert 'SUCCESS' in result


if __name__ == '__main__':
    pytest.main([__file__, '-v'])
