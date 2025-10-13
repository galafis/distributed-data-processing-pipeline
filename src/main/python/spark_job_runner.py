"""
Python wrapper for running Spark jobs.

This module provides a Python interface to execute Scala Spark jobs
and PySpark jobs with unified configuration and monitoring.

Author: Gabriel Demetrios Lafis
"""

import os
import sys
import logging
import subprocess
from typing import Dict, List, Optional
from pathlib import Path
import yaml


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class SparkJobRunner:
    """
    Runner for Spark jobs (both Scala and Python).
    
    Handles job submission, configuration, and monitoring.
    """
    
    def __init__(self, config_path: str = "config/pipeline.yaml"):
        """
        Initialize job runner.
        
        Parameters
        ----------
        config_path : str
            Path to configuration file
        """
        self.config_path = config_path
        self.config = self._load_config()
        self.spark_home = os.environ.get('SPARK_HOME', '/opt/spark')
        
    def _load_config(self) -> Dict:
        """Load configuration from YAML file."""
        if os.path.exists(self.config_path):
            with open(self.config_path, 'r') as f:
                return yaml.safe_load(f)
        return {}
    
    def submit_scala_job(
        self,
        job_class: str,
        jar_path: str,
        args: Optional[List[str]] = None,
        spark_conf: Optional[Dict[str, str]] = None
    ) -> int:
        """
        Submit a Scala Spark job.
        
        Parameters
        ----------
        job_class : str
            Fully qualified class name
        jar_path : str
            Path to JAR file
        args : list, optional
            Job arguments
        spark_conf : dict, optional
            Additional Spark configuration
            
        Returns
        -------
        return_code : int
            Job return code
        """
        logger.info(f"Submitting Scala job: {job_class}")
        
        cmd = [
            f"{self.spark_home}/bin/spark-submit",
            "--class", job_class,
            "--master", self.config.get('spark', {}).get('master', 'local[*]'),
            "--deploy-mode", "client",
        ]
        
        # Add Spark configuration
        if spark_conf:
            for key, value in spark_conf.items():
                cmd.extend(["--conf", f"{key}={value}"])
        
        # Add JAR
        cmd.append(jar_path)
        
        # Add job arguments
        if args:
            cmd.extend(args)
        
        logger.info(f"Command: {' '.join(cmd)}")
        
        # Execute
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        if result.returncode == 0:
            logger.info("Job completed successfully")
        else:
            logger.error(f"Job failed with return code {result.returncode}")
            logger.error(f"Error: {result.stderr}")
        
        return result.returncode
    
    def submit_pyspark_job(
        self,
        script_path: str,
        args: Optional[List[str]] = None,
        spark_conf: Optional[Dict[str, str]] = None,
        py_files: Optional[List[str]] = None
    ) -> int:
        """
        Submit a PySpark job.
        
        Parameters
        ----------
        script_path : str
            Path to Python script
        args : list, optional
            Job arguments
        spark_conf : dict, optional
            Additional Spark configuration
        py_files : list, optional
            Additional Python files to distribute
            
        Returns
        -------
        return_code : int
            Job return code
        """
        logger.info(f"Submitting PySpark job: {script_path}")
        
        cmd = [
            f"{self.spark_home}/bin/spark-submit",
            "--master", self.config.get('spark', {}).get('master', 'local[*]'),
            "--deploy-mode", "client",
        ]
        
        # Add Spark configuration
        if spark_conf:
            for key, value in spark_conf.items():
                cmd.extend(["--conf", f"{key}={value}"])
        
        # Add Python files
        if py_files:
            cmd.extend(["--py-files", ",".join(py_files)])
        
        # Add script
        cmd.append(script_path)
        
        # Add arguments
        if args:
            cmd.extend(args)
        
        logger.info(f"Command: {' '.join(cmd)}")
        
        # Execute
        result = subprocess.run(cmd, capture_output=True, text=True)
        
        if result.returncode == 0:
            logger.info("Job completed successfully")
        else:
            logger.error(f"Job failed with return code {result.returncode}")
            logger.error(f"Error: {result.stderr}")
        
        return result.returncode


def main():
    """Main entry point for CLI."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Spark Job Runner')
    parser.add_argument('--job-type', choices=['scala', 'pyspark'], required=True,
                       help='Type of job to run')
    parser.add_argument('--job-class', help='Scala job class (for Scala jobs)')
    parser.add_argument('--jar-path', help='Path to JAR file (for Scala jobs)')
    parser.add_argument('--script-path', help='Path to Python script (for PySpark jobs)')
    parser.add_argument('--args', nargs='*', help='Job arguments')
    parser.add_argument('--config', default='config/pipeline.yaml',
                       help='Path to configuration file')
    
    args = parser.parse_args()
    
    runner = SparkJobRunner(config_path=args.config)
    
    if args.job_type == 'scala':
        if not args.job_class or not args.jar_path:
            parser.error("--job-class and --jar-path are required for Scala jobs")
        return_code = runner.submit_scala_job(
            job_class=args.job_class,
            jar_path=args.jar_path,
            args=args.args
        )
    else:
        if not args.script_path:
            parser.error("--script-path is required for PySpark jobs")
        return_code = runner.submit_pyspark_job(
            script_path=args.script_path,
            args=args.args
        )
    
    sys.exit(return_code)


if __name__ == "__main__":
    main()

