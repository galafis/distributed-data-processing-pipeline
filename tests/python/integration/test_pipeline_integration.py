"""
Integration tests for the complete data pipeline.

Author: Gabriel Demetrios Lafis
"""

import os
import pytest
import tempfile
import shutil
from pathlib import Path

# Check if PySpark is available for real integration tests
try:
    import pyspark
    from pyspark.sql import SparkSession
    PYSPARK_AVAILABLE = not isinstance(pyspark, type(None)) and hasattr(SparkSession, 'builder')
except (ImportError, AttributeError):
    PYSPARK_AVAILABLE = False

pytestmark = pytest.mark.skipif(not PYSPARK_AVAILABLE, reason="PySpark not available for integration tests")


@pytest.mark.integration
class TestPipelineIntegration:
    """Integration tests for the complete pipeline."""
    
    @pytest.fixture(scope="class")
    def test_data_dir(self):
        """Create a temporary directory for test data."""
        temp_dir = tempfile.mkdtemp(prefix='pipeline_integration_test_')
        yield temp_dir
        # Cleanup
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
    
    def test_end_to_end_pipeline(self, test_data_dir):
        """Test end-to-end pipeline execution."""
        from pyspark.sql import SparkSession
        from pyspark.sql.functions import col
        
        # Create Spark session
        spark = SparkSession.builder \
            .appName("IntegrationTest") \
            .master("local[*]") \
            .config("spark.ui.enabled", "false") \
            .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension") \
            .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog") \
            .getOrCreate()
        
        # Create test data
        test_data = [
            ("tx1", "cust1", "prod1", 100.0, 2, 1609459200, "US", "Electronics"),
            ("tx2", "cust2", "prod2", 50.0, 1, 1609459200, "UK", "Books"),
            ("tx3", "cust1", "prod3", 75.0, 3, 1609459200, "US", "Clothing"),
        ]
        
        columns = ["transactionId", "customerId", "productId", "amount", "quantity", "timestamp", "country", "category"]
        df = spark.createDataFrame(test_data, columns)
        
        # Write test data
        input_path = os.path.join(test_data_dir, "input")
        df.write.mode("overwrite").parquet(input_path)
        
        # Verify data was written
        assert os.path.exists(input_path)
        
        # Read and validate
        read_df = spark.read.parquet(input_path)
        assert read_df.count() == 3
        
        # Transform data (simplified version of pipeline)
        transformed_df = read_df \
            .withColumn("total_amount", col("amount") * col("quantity")) \
            .filter(col("amount") > 0) \
            .filter(col("quantity") > 0)
        
        # Write output
        output_path = os.path.join(test_data_dir, "output")
        transformed_df.write.mode("overwrite").parquet(output_path)
        
        # Verify output
        output_df = spark.read.parquet(output_path)
        assert output_df.count() == 3
        assert "total_amount" in output_df.columns
        
        spark.stop()
    
    def test_data_quality_checks(self, test_data_dir):
        """Test data quality validation."""
        from pyspark.sql import SparkSession
        
        spark = SparkSession.builder \
            .appName("DataQualityTest") \
            .master("local[*]") \
            .config("spark.ui.enabled", "false") \
            .getOrCreate()
        
        # Create test data with quality issues
        test_data = [
            ("tx1", "cust1", "prod1", 100.0, 2),
            (None, "cust2", "prod2", 50.0, 1),  # null transaction ID
            ("tx3", "cust3", "prod3", -75.0, 3),  # negative amount
        ]
        
        columns = ["transactionId", "customerId", "productId", "amount", "quantity"]
        df = spark.createDataFrame(test_data, columns)
        
        # Quality checks
        total_count = df.count()
        null_tx_count = df.filter(df.transactionId.isNull()).count()
        negative_amount_count = df.filter(df.amount < 0).count()
        
        assert total_count == 3
        assert null_tx_count == 1
        assert negative_amount_count == 1
        
        # Clean data
        clean_df = df \
            .filter(df.transactionId.isNotNull()) \
            .filter(df.amount > 0)
        
        assert clean_df.count() == 1
        
        spark.stop()


if __name__ == '__main__':
    pytest.main([__file__, '-v', '-m', 'integration'])
