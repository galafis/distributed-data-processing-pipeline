#!/bin/bash
#
# Performance test runner for Distributed Data Processing Pipeline
#
# This script runs performance benchmarks to measure throughput and latency.
#
# Author: Gabriel Demetrios Lafis
#

set -e

echo "============================================"
echo "Running Performance Tests"
echo "============================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Project root: $PROJECT_ROOT"
echo ""

# Configuration
SMALL_SIZE=10000
MEDIUM_SIZE=100000
LARGE_SIZE=1000000

echo -e "${YELLOW}Setting up performance test environment...${NC}"
cd "$PROJECT_ROOT"

# Create test directories
mkdir -p /tmp/perf-test/data/input
mkdir -p /tmp/perf-test/data/output
mkdir -p /tmp/perf-test/results

# Function to generate test data
generate_test_data() {
    local size=$1
    local output_path=$2
    
    echo -e "${BLUE}Generating $size records...${NC}"
    
    python3 - <<EOF
import json
import random
from datetime import datetime, timedelta
import time

start_time = time.time()

transactions = []
for i in range($size):
    tx = {
        "transactionId": f"tx_{i}",
        "customerId": f"cust_{random.randint(1, 10000)}",
        "productId": f"prod_{random.randint(1, 1000)}",
        "amount": round(random.uniform(10.0, 1000.0), 2),
        "quantity": random.randint(1, 20),
        "timestamp": int((datetime.now() - timedelta(days=random.randint(0, 365))).timestamp()),
        "country": random.choice(["US", "UK", "CA", "AU", "DE", "FR", "JP", "BR", "IN", "CN"]),
        "category": random.choice(["Electronics", "Books", "Clothing", "Food", "Home", "Sports", "Toys", "Beauty"])
    }
    transactions.append(tx)

# Write as JSON
with open('$output_path/transactions.json', 'w') as f:
    for tx in transactions:
        f.write(json.dumps(tx) + '\n')

elapsed_time = time.time() - start_time
print(f"Generated $size records in {elapsed_time:.2f} seconds")
print(f"Generation rate: {$size / elapsed_time:.2f} records/sec")
EOF
}

# Function to convert to Parquet
convert_to_parquet() {
    local json_path=$1
    local parquet_path=$2
    
    echo -e "${BLUE}Converting to Parquet...${NC}"
    
    python3 - <<EOF
from pyspark.sql import SparkSession
import time

spark = SparkSession.builder \
    .appName("PerfTestDataPrep") \
    .master("local[*]") \
    .config("spark.ui.enabled", "false") \
    .config("spark.sql.shuffle.partitions", "8") \
    .getOrCreate()

start_time = time.time()

df = spark.read.json('$json_path')
df.write.mode('overwrite').parquet('$parquet_path')

count = df.count()
elapsed_time = time.time() - start_time

print(f"Converted {count} records in {elapsed_time:.2f} seconds")
print(f"Conversion rate: {count / elapsed_time:.2f} records/sec")

spark.stop()
EOF
}

# Function to run performance test
run_performance_test() {
    local size=$1
    local test_name=$2
    local input_path=$3
    local output_path=$4
    
    echo ""
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}Performance Test: $test_name (${size} records)${NC}"
    echo -e "${YELLOW}========================================${NC}"
    
    # Generate data
    generate_test_data $size "$input_path"
    convert_to_parquet "$input_path/transactions.json" "$input_path/transactions"
    
    # Run processing benchmark
    echo -e "${BLUE}Running processing benchmark...${NC}"
    
    python3 - <<EOF
from pyspark.sql import SparkSession
from pyspark.sql.functions import *
import time

spark = SparkSession.builder \
    .appName("PerfTest_$test_name") \
    .master("local[*]") \
    .config("spark.ui.enabled", "false") \
    .config("spark.sql.shuffle.partitions", "8") \
    .config("spark.sql.adaptive.enabled", "true") \
    .getOrCreate()

print("\n--- Processing Benchmark ---")

# Read
start_time = time.time()
df = spark.read.parquet('$input_path/transactions')
read_time = time.time() - start_time
print(f"Read time: {read_time:.2f} seconds")

# Transform
start_time = time.time()
transformed = df \
    .withColumn("total_amount", col("amount") * col("quantity")) \
    .withColumn("date", to_date(from_unixtime(col("timestamp")))) \
    .filter(col("amount") > 0) \
    .filter(col("quantity") > 0) \
    .dropDuplicates(["transactionId"])
transform_time = time.time() - start_time
print(f"Transform time: {transform_time:.2f} seconds")

# Aggregate
start_time = time.time()
aggregated = transformed.groupBy("country", "category") \
    .agg(
        count("*").alias("transaction_count"),
        sum("total_amount").alias("total_revenue"),
        avg("total_amount").alias("avg_revenue")
    )
agg_time = time.time() - start_time
print(f"Aggregation time: {agg_time:.2f} seconds")

# Write
start_time = time.time()
transformed.write.mode('overwrite').parquet('$output_path/detail')
write_time = time.time() - start_time
print(f"Write time: {write_time:.2f} seconds")

# Calculate metrics
total_time = read_time + transform_time + agg_time + write_time
record_count = transformed.count()
throughput = record_count / total_time if total_time > 0 else 0

print(f"\n--- Summary ---")
print(f"Total records processed: {record_count}")
print(f"Total processing time: {total_time:.2f} seconds")
print(f"Throughput: {throughput:.2f} records/sec")
print(f"Throughput: {throughput * 60:.2f} records/min")

# Write results to file
with open('/tmp/perf-test/results/${test_name}_results.txt', 'w') as f:
    f.write(f"Test: $test_name\n")
    f.write(f"Records: $size\n")
    f.write(f"Read time: {read_time:.2f}s\n")
    f.write(f"Transform time: {transform_time:.2f}s\n")
    f.write(f"Aggregation time: {agg_time:.2f}s\n")
    f.write(f"Write time: {write_time:.2f}s\n")
    f.write(f"Total time: {total_time:.2f}s\n")
    f.write(f"Throughput: {throughput:.2f} records/sec\n")

spark.stop()
EOF
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Performance test completed${NC}"
    else
        echo -e "${RED}✗ Performance test failed${NC}"
        return 1
    fi
}

# Run tests
echo -e "${YELLOW}Starting performance benchmarks...${NC}"
echo ""

# Small dataset test
run_performance_test $SMALL_SIZE "small" "/tmp/perf-test/data/input/small" "/tmp/perf-test/data/output/small"

# Medium dataset test
run_performance_test $MEDIUM_SIZE "medium" "/tmp/perf-test/data/input/medium" "/tmp/perf-test/data/output/medium"

# Large dataset test (optional, can be commented out for faster tests)
if [ "${RUN_LARGE_TEST:-false}" = "true" ]; then
    run_performance_test $LARGE_SIZE "large" "/tmp/perf-test/data/input/large" "/tmp/perf-test/data/output/large"
fi

# Generate summary report
echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Performance Test Summary${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

for result_file in /tmp/perf-test/results/*.txt; do
    if [ -f "$result_file" ]; then
        echo -e "${BLUE}$(basename $result_file .txt):${NC}"
        cat "$result_file"
        echo ""
    fi
done

# Cleanup
echo -e "${YELLOW}Cleaning up test environment...${NC}"
rm -rf /tmp/perf-test
echo -e "${GREEN}✓ Cleanup completed${NC}"

echo ""
echo "============================================"
echo -e "${GREEN}Performance Tests Completed!${NC}"
echo "============================================"
echo ""
echo -e "${BLUE}Note: For comprehensive benchmarks, run on a cluster with:${NC}"
echo "  export RUN_LARGE_TEST=true"
echo "  ./scripts/run_performance_tests.sh"
