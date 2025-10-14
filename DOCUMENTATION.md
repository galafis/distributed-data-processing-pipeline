# Project Documentation

## Distributed Data Processing Pipeline

### Overview

This document provides comprehensive technical documentation for the Distributed Data Processing Pipeline project.

---

## Table of Contents

1. [Architecture](#architecture)
2. [Components](#components)
3. [Setup & Installation](#setup--installation)
4. [Usage Examples](#usage-examples)
5. [Testing](#testing)
6. [Deployment](#deployment)
7. [Troubleshooting](#troubleshooting)

---

## Architecture

### High-Level Architecture

The pipeline follows a medallion architecture (Bronze -> Silver -> Gold) with three main layers:

1. **Data Ingestion Layer**: Reads from various sources
2. **Processing Layer**: Spark-based transformations
3. **Storage Layer**: Delta Lake for ACID transactions
4. **Orchestration Layer**: Airflow for workflow management

### Technology Stack

- **Processing Engine**: Apache Spark 3.5
- **Programming Languages**: Scala 2.12, Python 3.8+
- **Storage Format**: Delta Lake 2.4
- **Orchestration**: Apache Airflow 2.7
- **Containerization**: Docker

---

## Components

### Scala Components

#### Core Package (`com.gabriellafis.pipeline.core`)

- **BaseSparkJob**: Abstract base for all Spark jobs
- **SparkSessionBuilder**: Centralized Spark session management
- **ConfigManager**: Configuration management

#### Jobs Package (`com.gabriellafis.pipeline.jobs`)

- **BatchETLJob**: Batch data processing
- **StreamingJob**: Real-time stream processing

#### Transformations Package (`com.gabriellafis.pipeline.transformations`)

- **Cleansing**: Data cleaning operations
- **Enrichment**: Data enrichment operations
- **Aggregations**: Aggregation operations

#### Utils Package (`com.gabriellafis.pipeline.utils`)

- **DataValidator**: Data quality validation
- **MetricsCollector**: Performance metrics collection

### Python Components

#### Main Package (`src/main/python`)

- **spark_job_runner.py**: Spark job submission wrapper
- **data_quality_checks.py**: Quality validation implementations

#### Utils Package (`src/main/python/utils`)

- **s3_utils.py**: AWS S3 operations
- **db_utils.py**: Database operations

---

## Setup & Installation

### Prerequisites

```bash
# Install Java
sudo apt-get install openjdk-11-jdk

# Install Scala
curl -fL https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz | gzip -d > cs
chmod +x cs
./cs setup

# Install SBT
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo apt-key add
sudo apt-get update
sudo apt-get install sbt
```

### Build Project

```bash
# Clone repository
git clone https://github.com/galafis/distributed-data-processing-pipeline.git
cd distributed-data-processing-pipeline

# Build Scala project
sbt clean compile assembly

# Install Python dependencies
pip install -r requirements.txt
```

### Docker Setup

```bash
# Build Docker image
docker build -t pipeline:latest -f docker/Dockerfile .

# Start services
docker-compose up -d

# Check status
docker-compose ps
```

---

## Usage Examples

### Running Batch ETL Job

#### Local Mode

```bash
spark-submit \
  --class com.gabriellafis.pipeline.jobs.BatchETLJob \
  --master local[*] \
  target/scala-2.12/distributed-data-processing-pipeline-1.0.0.jar \
  /path/to/input \
  /path/to/output
```

#### Cluster Mode (YARN)

```bash
spark-submit \
  --class com.gabriellafis.pipeline.jobs.BatchETLJob \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 10 \
  --executor-memory 8g \
  --executor-cores 4 \
  target/scala-2.12/distributed-data-processing-pipeline-1.0.0.jar \
  s3://bucket/input \
  s3://bucket/output
```

### Running Streaming Job

```bash
spark-submit \
  --class com.gabriellafis.pipeline.jobs.StreamingJob \
  --master local[*] \
  target/scala-2.12/distributed-data-processing-pipeline-1.0.0.jar \
  /path/to/stream/input \
  /path/to/stream/output
```

### Using Python Wrapper

```python
from spark_job_runner import SparkJobRunner

runner = SparkJobRunner(config_path='config/pipeline.yaml')

# Submit Scala job
runner.submit_scala_job(
    job_class='com.gabriellafis.pipeline.jobs.BatchETLJob',
    jar_path='target/scala-2.12/distributed-data-processing-pipeline-1.0.0.jar',
    args=['input_path', 'output_path']
)
```

---

## Testing

### Unit Tests

#### Scala Tests

```bash
# Run all tests
sbt test

# Run specific test
sbt "testOnly *BatchETLJobSpec"

# Run with coverage
sbt clean coverage test coverageReport
```

#### Python Tests

```bash
# Run all tests
pytest tests/python/

# Run specific test file
pytest tests/python/unit/test_spark_job_runner.py

# Run with coverage
pytest tests/python/ --cov=src/main/python --cov-report=html
```

### Integration Tests

```bash
# Run integration test suite
./scripts/run_integration_tests.sh

# Run specific integration test
pytest tests/python/integration/test_pipeline_integration.py -v
```

### Performance Tests

```bash
# Run performance benchmarks
./scripts/run_performance_tests.sh

# Run with large dataset
RUN_LARGE_TEST=true ./scripts/run_performance_tests.sh
```

---

## Deployment

### AWS EMR Deployment

1. **Create EMR Cluster**

```bash
aws emr create-cluster \
  --name "DataPipeline" \
  --release-label emr-6.10.0 \
  --applications Name=Spark Name=Hadoop \
  --ec2-attributes KeyName=mykey \
  --instance-type m5.4xlarge \
  --instance-count 3
```

2. **Upload JAR to S3**

```bash
aws s3 cp target/scala-2.12/distributed-data-processing-pipeline-1.0.0.jar \
  s3://my-bucket/jars/
```

3. **Submit Job**

```bash
aws emr add-steps \
  --cluster-id j-XXXXXXXXXXXXX \
  --steps Type=Spark,Name="BatchETL",ActionOnFailure=CONTINUE,\
Args=[--class,com.gabriellafis.pipeline.jobs.BatchETLJob,\
s3://my-bucket/jars/distributed-data-processing-pipeline-1.0.0.jar,\
s3://my-bucket/input,s3://my-bucket/output]
```

### Kubernetes Deployment

```bash
# Build and push image
docker build -t myregistry/pipeline:v1.0 .
docker push myregistry/pipeline:v1.0

# Deploy with Spark operator
kubectl apply -f k8s/spark-operator.yaml
kubectl apply -f k8s/batch-job.yaml
```

### Airflow Deployment

```bash
# Copy DAGs to Airflow
cp dags/*.py $AIRFLOW_HOME/dags/

# Trigger DAG
airflow dags trigger distributed_data_processing_pipeline
```

---

## Troubleshooting

### Common Issues

#### OutOfMemoryError

**Problem**: Spark executor runs out of memory

**Solution**:
```bash
spark-submit \
  --executor-memory 16g \
  --driver-memory 8g \
  --conf spark.memory.fraction=0.8 \
  ...
```

#### Slow Performance

**Problem**: Job runs slowly

**Solutions**:
1. Enable adaptive query execution
2. Increase parallelism
3. Use broadcast joins for small tables

```bash
spark-submit \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.sql.shuffle.partitions=200 \
  --conf spark.sql.autoBroadcastJoinThreshold=10485760 \
  ...
```

#### Delta Lake Conflicts

**Problem**: Concurrent writes causing conflicts

**Solution**: Use optimistic concurrency control
```scala
deltaTable.as("target")
  .merge(updates.as("source"), "target.id = source.id")
  .whenMatched.updateAll()
  .whenNotMatched.insertAll()
  .execute()
```

### Debugging

#### Enable Debug Logging

```bash
# In spark-submit
--conf spark.log.level=DEBUG

# In code
spark.sparkContext.setLogLevel("DEBUG")
```

#### View Spark UI

```bash
# Local mode
http://localhost:4040

# Cluster mode
# Access through Spark history server
```

#### Check Airflow Logs

```bash
# View task logs
airflow tasks logs distributed_data_processing_pipeline check_data_availability 2024-01-01

# View scheduler logs
tail -f $AIRFLOW_HOME/logs/scheduler/latest/*.log
```

---

## Performance Tuning

### Memory Configuration

```properties
spark.executor.memory=8g
spark.driver.memory=4g
spark.memory.fraction=0.6
spark.memory.storageFraction=0.5
```

### Shuffle Configuration

```properties
spark.sql.shuffle.partitions=200
spark.shuffle.compress=true
spark.shuffle.spill.compress=true
```

### Delta Lake Optimization

```scala
// Optimize table
deltaTable.optimize().executeCompaction()

// Z-order
deltaTable.optimize().executeZOrderBy("country", "date")

// Vacuum old versions
deltaTable.vacuum(168) // 7 days
```

---

## Monitoring

### Metrics Collection

The `MetricsCollector` class provides built-in metrics:

```scala
val metrics = new MetricsCollector(spark)

metrics.startTiming("etl_job")
// ... perform work
metrics.endTiming("etl_job")

metrics.recordMetric("records_processed", df.count())
metrics.printMetrics()
```

### Health Checks

```bash
# Check Spark application status
spark-submit --status <submission-id>

# Check Airflow DAG status
airflow dags list

# Check Docker services
docker-compose ps
```

---

## Best Practices

1. **Use Delta Lake for ACID guarantees**
2. **Implement comprehensive data quality checks**
3. **Use partitioning strategically**
4. **Enable adaptive query execution**
5. **Monitor job performance**
6. **Version your data with Delta Lake time travel**
7. **Use broadcast joins for small tables**
8. **Implement proper error handling**
9. **Test with realistic data volumes**
10. **Document your code**

---

## Performance Tuning

### Spark Configuration

```scala
// Recommended production settings
spark.conf.set("spark.sql.adaptive.enabled", "true")
spark.conf.set("spark.sql.adaptive.coalescePartitions.enabled", "true")
spark.conf.set("spark.sql.adaptive.skewJoin.enabled", "true")
spark.conf.set("spark.sql.shuffle.partitions", "200")
spark.conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
spark.conf.set("spark.sql.files.maxPartitionBytes", "134217728") // 128MB
```

### Memory Tuning

```bash
# Executor memory distribution
# Total = Execution (60%) + Storage (40%)
# Reserve 300MB for overhead

spark-submit \
  --executor-memory 8g \
  --executor-cores 4 \
  --driver-memory 4g \
  --conf spark.memory.fraction=0.8 \
  --conf spark.memory.storageFraction=0.4
```

### Partitioning Strategy

```scala
// Write with optimal partitioning
df.repartition($"date", $"region")
  .write
  .partitionBy("date", "region")
  .format("delta")
  .save(path)

// For small files, use coalesce
df.coalesce(10).write.parquet(path)
```

---

## Common Issues and Solutions

### Issue: OutOfMemoryError

**Symptoms:** Jobs fail with OOM errors

**Solutions:**
1. Increase executor memory
2. Reduce `spark.sql.shuffle.partitions`
3. Use `.persist()` strategically
4. Enable dynamic allocation

```bash
spark-submit \
  --executor-memory 12g \
  --conf spark.dynamicAllocation.enabled=true \
  --conf spark.dynamicAllocation.minExecutors=2 \
  --conf spark.dynamicAllocation.maxExecutors=20
```

### Issue: Data Skew

**Symptoms:** Some tasks take much longer than others

**Solutions:**
1. Enable AQE skew join optimization
2. Use salting technique
3. Repartition with custom logic

```scala
// Salt key to distribute skewed data
val saltedDf = df.withColumn("salt", (rand() * 10).cast("int"))
  .withColumn("salted_key", concat($"key", lit("_"), $"salt"))
```

### Issue: Small Files Problem

**Symptoms:** Too many small files causing slow reads

**Solutions:**
1. Use Delta Lake auto-compact
2. Optimize after writes
3. Control file size with maxRecordsPerFile

```scala
// Optimize table to consolidate files
deltaTable.optimize()
  .where("date >= '2024-01-01'")
  .executeCompaction()

// Z-order for query optimization
deltaTable.optimize()
  .executeZOrderBy("country", "product_id")
```

---

## Support

For issues and questions:
- GitHub Issues: [Create an issue](https://github.com/galafis/distributed-data-processing-pipeline/issues)
- Documentation: [README.md](README.md)
- Contributing: [CONTRIBUTING.md](CONTRIBUTING.md)

---

**Author**: Gabriel Demetrios Lafis  
**Version**: 1.0.0  
**Last Updated**: October 2025
