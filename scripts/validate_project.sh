#!/bin/bash
#
# Project Validation Script
# 
# Checks if all required files and directories are in place.
#
# Author: Gabriel Demetrios Lafis
#

set -e

echo "============================================"
echo "Project Validation"
echo "============================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

ERRORS=0
WARNINGS=0

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
    else
        echo -e "${RED}✗${NC} $1 (missing)"
        ((ERRORS++))
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
    else
        echo -e "${RED}✗${NC} $1 (missing)"
        ((ERRORS++))
    fi
}

check_optional() {
    if [ -f "$1" ] || [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $1"
    else
        echo -e "${YELLOW}⚠${NC} $1 (optional, not found)"
        ((WARNINGS++))
    fi
}

echo "Checking core files..."
check_file "README.md"
check_file "LICENSE"
check_file "CONTRIBUTING.md"
check_file "DOCUMENTATION.md"
check_file "build.sbt"
check_file "requirements.txt"
check_file "pytest.ini"
check_file ".gitignore"

echo ""
echo "Checking configuration files..."
check_file "config/pipeline.yaml"
check_file "docker-compose.yml"
check_file "docker/Dockerfile"

echo ""
echo "Checking Scala source files..."
check_dir "src/main/scala/com/gabriellafis/pipeline"
check_file "src/main/scala/com/gabriellafis/pipeline/core/BaseSparkJob.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/core/SparkSessionBuilder.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/core/ConfigManager.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/jobs/BatchETLJob.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/jobs/StreamingJob.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/transformations/Cleansing.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/transformations/Enrichment.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/transformations/Aggregations.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/utils/DataValidator.scala"
check_file "src/main/scala/com/gabriellafis/pipeline/utils/MetricsCollector.scala"

echo ""
echo "Checking Scala tests..."
check_dir "src/test/scala/com/gabriellafis/pipeline"
check_file "src/test/scala/com/gabriellafis/pipeline/core/BaseSparkJobSpec.scala"
check_file "src/test/scala/com/gabriellafis/pipeline/jobs/BatchETLJobSpec.scala"

echo ""
echo "Checking Python source files..."
check_file "src/main/python/spark_job_runner.py"
check_file "src/main/python/data_quality_checks.py"
check_file "src/main/python/utils/__init__.py"
check_file "src/main/python/utils/s3_utils.py"
check_file "src/main/python/utils/db_utils.py"

echo ""
echo "Checking Python tests..."
check_dir "tests/python"
check_file "tests/python/__init__.py"
check_file "tests/python/unit/__init__.py"
check_file "tests/python/unit/test_spark_job_runner.py"
check_file "tests/python/unit/test_data_pipeline_dag.py"
check_file "tests/python/integration/__init__.py"
check_file "tests/python/integration/test_pipeline_integration.py"

echo ""
echo "Checking Airflow DAGs..."
check_file "dags/data_pipeline_dag.py"

echo ""
echo "Checking scripts..."
check_file "scripts/run_integration_tests.sh"
check_file "scripts/run_performance_tests.sh"
check_file "scripts/generate_quality_report.py"

echo ""
echo "Checking CI/CD..."
check_file ".github/workflows/ci.yml"

echo ""
echo "Checking data directories..."
check_dir "data"
check_file "data/raw/.gitkeep"
check_file "data/processed/.gitkeep"
check_file "data/staging/.gitkeep"

echo ""
echo "Checking optional files..."
check_optional "notebooks/data_exploration_example.ipynb"
check_optional "project/build.properties"
check_optional "project/plugins.sbt"

echo ""
echo "============================================"
echo "Validation Summary"
echo "============================================"

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}✓ All required files present${NC}"
else
    echo -e "${RED}✗ Found $ERRORS missing required files${NC}"
fi

if [ $WARNINGS -gt 0 ]; then
    echo -e "${YELLOW}⚠ Found $WARNINGS missing optional files${NC}"
fi

echo ""

if [ $ERRORS -eq 0 ]; then
    echo -e "${GREEN}Project structure is valid!${NC}"
    exit 0
else
    echo -e "${RED}Project structure has issues!${NC}"
    exit 1
fi
