"""
Unit tests for SparkJobRunner.

Author: Gabriel Demetrios Lafis
"""

import os
import pytest
import tempfile
import yaml
from pathlib import Path
from unittest.mock import Mock, patch, MagicMock
import sys

# Add src to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '../../..', 'src/main/python'))

from spark_job_runner import SparkJobRunner


class TestSparkJobRunner:
    """Test suite for SparkJobRunner class."""
    
    @pytest.fixture
    def temp_config(self):
        """Create a temporary config file for testing."""
        config = {
            'spark': {
                'master': 'local[*]',
                'app_name': 'TestApp'
            },
            'pipeline': {
                'input': {
                    'path': '/tmp/input',
                    'format': 'parquet'
                },
                'output': {
                    'path': '/tmp/output',
                    'format': 'delta'
                }
            }
        }
        
        with tempfile.NamedTemporaryFile(mode='w', suffix='.yaml', delete=False) as f:
            yaml.dump(config, f)
            config_path = f.name
        
        yield config_path
        
        # Cleanup
        if os.path.exists(config_path):
            os.remove(config_path)
    
    def test_init(self, temp_config):
        """Test SparkJobRunner initialization."""
        runner = SparkJobRunner(config_path=temp_config)
        
        assert runner is not None
        assert runner.config is not None
        assert 'spark' in runner.config
        assert runner.spark_home is not None
    
    def test_load_config(self, temp_config):
        """Test config loading."""
        runner = SparkJobRunner(config_path=temp_config)
        
        assert runner.config['spark']['master'] == 'local[*]'
        assert runner.config['spark']['app_name'] == 'TestApp'
    
    def test_load_config_file_not_found(self):
        """Test config loading when file doesn't exist."""
        runner = SparkJobRunner(config_path='nonexistent.yaml')
        
        assert runner.config == {}
    
    @patch('subprocess.run')
    def test_submit_scala_job_success(self, mock_run, temp_config):
        """Test successful Scala job submission."""
        mock_result = Mock()
        mock_result.returncode = 0
        mock_result.stdout = "Job completed"
        mock_result.stderr = ""
        mock_run.return_value = mock_result
        
        runner = SparkJobRunner(config_path=temp_config)
        return_code = runner.submit_scala_job(
            job_class='com.example.TestJob',
            jar_path='/path/to/test.jar',
            args=['arg1', 'arg2']
        )
        
        assert return_code == 0
        assert mock_run.called
        
        # Verify command structure
        call_args = mock_run.call_args[0][0]
        assert 'spark-submit' in call_args[0]
        assert '--class' in call_args
        assert 'com.example.TestJob' in call_args
    
    @patch('subprocess.run')
    def test_submit_scala_job_failure(self, mock_run, temp_config):
        """Test failed Scala job submission."""
        mock_result = Mock()
        mock_result.returncode = 1
        mock_result.stdout = ""
        mock_result.stderr = "Error occurred"
        mock_run.return_value = mock_result
        
        runner = SparkJobRunner(config_path=temp_config)
        return_code = runner.submit_scala_job(
            job_class='com.example.TestJob',
            jar_path='/path/to/test.jar'
        )
        
        assert return_code == 1
    
    @patch('subprocess.run')
    def test_submit_scala_job_with_spark_conf(self, mock_run, temp_config):
        """Test Scala job submission with Spark configuration."""
        mock_result = Mock()
        mock_result.returncode = 0
        mock_result.stdout = ""
        mock_result.stderr = ""
        mock_run.return_value = mock_result
        
        runner = SparkJobRunner(config_path=temp_config)
        spark_conf = {
            'spark.executor.memory': '4g',
            'spark.executor.cores': '2'
        }
        
        return_code = runner.submit_scala_job(
            job_class='com.example.TestJob',
            jar_path='/path/to/test.jar',
            spark_conf=spark_conf
        )
        
        assert return_code == 0
        
        # Verify Spark configuration in command
        call_args = mock_run.call_args[0][0]
        assert '--conf' in call_args
        assert any('spark.executor.memory=4g' in arg for arg in call_args)
    
    @patch('subprocess.run')
    def test_submit_pyspark_job_success(self, mock_run, temp_config):
        """Test successful PySpark job submission."""
        mock_result = Mock()
        mock_result.returncode = 0
        mock_result.stdout = "Job completed"
        mock_result.stderr = ""
        mock_run.return_value = mock_result
        
        runner = SparkJobRunner(config_path=temp_config)
        return_code = runner.submit_pyspark_job(
            script_path='/path/to/script.py',
            args=['arg1', 'arg2']
        )
        
        assert return_code == 0
        assert mock_run.called
        
        # Verify command structure
        call_args = mock_run.call_args[0][0]
        assert 'spark-submit' in call_args[0]
        assert '/path/to/script.py' in call_args
    
    @patch('subprocess.run')
    def test_submit_pyspark_job_with_py_files(self, mock_run, temp_config):
        """Test PySpark job submission with additional Python files."""
        mock_result = Mock()
        mock_result.returncode = 0
        mock_result.stdout = ""
        mock_result.stderr = ""
        mock_run.return_value = mock_result
        
        runner = SparkJobRunner(config_path=temp_config)
        py_files = ['/path/to/lib1.py', '/path/to/lib2.py']
        
        return_code = runner.submit_pyspark_job(
            script_path='/path/to/script.py',
            py_files=py_files
        )
        
        assert return_code == 0
        
        # Verify py-files in command
        call_args = mock_run.call_args[0][0]
        assert '--py-files' in call_args
        py_files_arg = call_args[call_args.index('--py-files') + 1]
        assert '/path/to/lib1.py' in py_files_arg
        assert '/path/to/lib2.py' in py_files_arg


if __name__ == '__main__':
    pytest.main([__file__, '-v'])
