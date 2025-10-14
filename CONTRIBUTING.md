# Contributing to Distributed Data Processing Pipeline

First off, thank you for considering contributing to this project! 🎉

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [How to Contribute](#how-to-contribute)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Pull Request Process](#pull-request-process)
- [Community](#community)

## Code of Conduct

This project adheres to a code of conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/distributed-data-processing-pipeline.git
   cd distributed-data-processing-pipeline
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/galafis/distributed-data-processing-pipeline.git
   ```

## Development Setup

### Prerequisites

- Java 11+
- Scala 2.12
- Python 3.8+
- Apache Spark 3.5+
- SBT (Scala Build Tool)
- Docker (optional, for containerized development)

### Setup Steps

1. **Build Scala project**:
   ```bash
   sbt clean compile
   ```

2. **Install Python dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

3. **Run tests** to ensure everything is working:
   ```bash
   # Scala tests
   sbt test
   
   # Python tests
   pytest tests/python/
   ```

## How to Contribute

### Reporting Bugs

Before creating bug reports, please check existing issues to avoid duplicates. When creating a bug report, include:

- **Clear title and description**
- **Steps to reproduce** the issue
- **Expected vs actual behavior**
- **Environment details** (OS, Spark version, etc.)
- **Code samples** or error messages

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Clear title and description**
- **Use case** explaining why this would be useful
- **Potential implementation** details (if you have ideas)
- **Examples** of how it would work

### Your First Code Contribution

Unsure where to begin? Look for issues tagged with:
- `good first issue` - Good for newcomers
- `help wanted` - Issues where we need community help

## Coding Standards

### Scala

- Follow [Scala Style Guide](https://docs.scala-lang.org/style/)
- Use **2 spaces** for indentation
- Maximum line length: **120 characters**
- Use **meaningful variable names**
- Add **ScalaDoc** comments for public methods
- Use **functional programming** patterns where appropriate

Example:
```scala
/**
 * Process transaction data.
 * 
 * @param df Input DataFrame
 * @return Processed DataFrame
 */
def processTransactions(df: DataFrame): DataFrame = {
  df.filter(col("amount") > 0)
    .withColumn("total", col("amount") * col("quantity"))
}
```

### Python

- Follow [PEP 8](https://www.python.org/dev/peps/pep-0008/) style guide
- Use **4 spaces** for indentation
- Maximum line length: **100 characters**
- Add **docstrings** for all functions and classes
- Use **type hints** where possible

Example:
```python
def process_data(input_path: str, output_path: str) -> int:
    """
    Process data from input to output.
    
    Parameters
    ----------
    input_path : str
        Path to input data
    output_path : str
        Path to output location
        
    Returns
    -------
    int
        Number of records processed
    """
    # Implementation here
    pass
```

### Git Commit Messages

- Use the **present tense** ("Add feature" not "Added feature")
- Use the **imperative mood** ("Move cursor to..." not "Moves cursor to...")
- **Limit the first line** to 72 characters or less
- Reference issues and pull requests liberally after the first line

Example:
```
Add batch processing optimizations

- Implement adaptive query execution
- Add partition coalescing
- Optimize broadcast joins

Fixes #123
```

## Testing Guidelines

### Unit Tests

- Write tests for **all new features**
- Maintain **high code coverage** (aim for >80%)
- Tests should be **fast** and **independent**
- Use **descriptive test names**

**Scala Tests** (ScalaTest):
```scala
"BatchETLJob" should "filter invalid records" in {
  val testData = Seq(
    ("tx1", 100.0),
    ("tx2", -50.0)  // invalid
  ).toDF("id", "amount")
  
  val result = BatchETLJob.filterValid(testData)
  result.count() shouldBe 1
}
```

**Python Tests** (pytest):
```python
def test_job_runner_success():
    """Test successful job execution."""
    runner = SparkJobRunner()
    result = runner.submit_job(...)
    assert result == 0
```

### Integration Tests

- Test **end-to-end workflows**
- Use **realistic test data**
- Clean up test artifacts after execution
- Mark slow tests appropriately:
  ```python
  @pytest.mark.integration
  @pytest.mark.slow
  def test_full_pipeline():
      # Test implementation
  ```

### Running Tests

```bash
# All tests
sbt test
pytest tests/python/

# Specific test suites
sbt "testOnly *BatchETLJobSpec"
pytest tests/python/unit/test_spark_job_runner.py

# Integration tests
./scripts/run_integration_tests.sh

# Performance tests
./scripts/run_performance_tests.sh
```

## Pull Request Process

1. **Create a feature branch**:
   ```bash
   git checkout -b feature/my-new-feature
   ```

2. **Make your changes** and commit them:
   ```bash
   git add .
   git commit -m "Add my new feature"
   ```

3. **Keep your branch updated**:
   ```bash
   git fetch upstream
   git rebase upstream/master
   ```

4. **Push to your fork**:
   ```bash
   git push origin feature/my-new-feature
   ```

5. **Create a Pull Request** on GitHub with:
   - **Clear title** describing the change
   - **Description** explaining what and why
   - **Reference to related issues**
   - **Test results** showing all tests pass
   - **Screenshots** for UI changes (if applicable)

6. **Address review feedback** if requested

7. **Merge requirements**:
   - All tests must pass
   - Code review approval from maintainers
   - No merge conflicts
   - Documentation updated (if needed)

## Community

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: General questions and discussions
- **Pull Requests**: Code contributions and reviews

### Recognition

Contributors will be recognized in:
- **README.md** acknowledgments section
- **Release notes** for significant contributions

## Questions?

Don't hesitate to ask questions! You can:
- Open an issue with the `question` label
- Start a discussion on GitHub Discussions
- Review existing documentation and examples

Thank you for contributing! 🚀

---

**Gabriel Demetrios Lafis**  
Project Maintainer
