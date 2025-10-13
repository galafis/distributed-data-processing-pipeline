"""
Airflow DAG for orchestrating the distributed data processing pipeline.

This DAG coordinates:
- Data ingestion
- Batch ETL processing
- Data quality checks
- Aggregations
- Notifications

Author: Gabriel Demetrios Lafis
"""

from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.bash import BashOperator
from airflow.providers.apache.spark.operators.spark_submit import SparkSubmitOperator
from airflow.utils.dates import days_ago


default_args = {
    'owner': 'Gabriel Demetrios Lafis',
    'depends_on_past': False,
    'email': ['alerts@example.com'],
    'email_on_failure': True,
    'email_on_retry': False,
    'retries': 3,
    'retry_delay': timedelta(minutes=5),
    'execution_timeout': timedelta(hours=2),
}


dag = DAG(
    'distributed_data_processing_pipeline',
    default_args=default_args,
    description='Distributed data processing pipeline with Spark and Delta Lake',
    schedule_interval='0 2 * * *',  # Daily at 2 AM
    start_date=days_ago(1),
    catchup=False,
    max_active_runs=1,
    tags=['data-engineering', 'spark', 'etl'],
)


def check_data_availability(**context):
    """Check if source data is available."""
    import os
    from pathlib import Path
    
    data_path = context['params']['input_path']
    
    if not os.path.exists(data_path):
        raise FileNotFoundError(f"Data not found at {data_path}")
    
    files = list(Path(data_path).rglob('*.parquet'))
    
    if len(files) == 0:
        raise ValueError("No parquet files found in input path")
    
    print(f"Found {len(files)} parquet files")
    return len(files)


def validate_output(**context):
    """Validate output data quality."""
    from pyspark.sql import SparkSession
    
    spark = SparkSession.builder \
        .appName("OutputValidation") \
        .getOrCreate()
    
    try:
        output_path = context['params']['output_path']
        
        # Read output data
        df = spark.read.format("delta").load(f"{output_path}/transactions_detail")
        
        # Validation checks
        record_count = df.count()
        null_count = df.filter(df.customerId.isNull()).count()
        
        print(f"Total records: {record_count}")
        print(f"Null customers: {null_count}")
        
        # Assert quality thresholds
        assert record_count > 0, "No records in output"
        assert null_count == 0, "Found null customer IDs in output"
        
        print("Output validation passed")
        
    finally:
        spark.stop()


def send_completion_notification(**context):
    """Send completion notification."""
    execution_date = context['execution_date']
    task_instance = context['task_instance']
    
    message = f"""
    Pipeline Execution Completed
    
    Execution Date: {execution_date}
    Status: SUCCESS
    
    Tasks Completed:
    - Data availability check
    - Batch ETL processing
    - Output validation
    
    Pipeline run completed successfully.
    """
    
    print(message)
    # In production, send email or Slack notification
    return message


# Task 1: Check data availability
check_data = PythonOperator(
    task_id='check_data_availability',
    python_callable=check_data_availability,
    params={
        'input_path': '/data/raw/transactions'
    },
    dag=dag,
)


# Task 2: Run batch ETL job
batch_etl = SparkSubmitOperator(
    task_id='batch_etl_job',
    application='/opt/pipeline/jars/distributed-data-processing-pipeline-1.0.0.jar',
    java_class='com.gabriellafis.pipeline.jobs.BatchETLJob',
    name='batch_etl_job',
    conf={
        'spark.sql.adaptive.enabled': 'true',
        'spark.sql.adaptive.coalescePartitions.enabled': 'true',
        'spark.serializer': 'org.apache.spark.serializer.KryoSerializer',
    },
    application_args=[
        '/data/raw/transactions',
        '/data/processed'
    ],
    dag=dag,
)


# Task 3: Validate output
validate_output_task = PythonOperator(
    task_id='validate_output',
    python_callable=validate_output,
    params={
        'output_path': '/data/processed'
    },
    dag=dag,
)


# Task 4: Generate data quality report
generate_report = BashOperator(
    task_id='generate_quality_report',
    bash_command="""
    echo "Generating data quality report..."
    python3 /opt/pipeline/scripts/generate_quality_report.py \
        --input-path /data/processed/transactions_detail \
        --output-path /data/reports/quality_report_{{ ds }}.html
    """,
    dag=dag,
)


# Task 5: Send notification
notify = PythonOperator(
    task_id='send_completion_notification',
    python_callable=send_completion_notification,
    dag=dag,
)


# Define task dependencies
check_data >> batch_etl >> validate_output_task >> generate_report >> notify

