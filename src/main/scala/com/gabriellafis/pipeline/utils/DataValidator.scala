package com.gabriellafis.pipeline.utils

import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.functions._
import org.slf4j.{Logger, LoggerFactory}

/**
 * Data validation utilities.
 * 
 * Provides common validation functions for data quality checks.
 * 
 * @author Gabriel Demetrios Lafis
 */
object DataValidator {
  
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  
  /**
   * Validate null values in specified columns.
   * 
   * @param df DataFrame to validate
   * @param columns Columns to check for nulls
   * @param threshold Maximum acceptable null percentage (0.0 to 1.0)
   * @return ValidationResult with details
   */
  def validateNulls(
    df: DataFrame,
    columns: Seq[String],
    threshold: Double = 0.01
  ): ValidationResult = {
    
    val totalRecords = df.count()
    val failures = scala.collection.mutable.ListBuffer[String]()
    
    columns.foreach { col =>
      val nullCount = df.filter(df(col).isNull).count()
      val nullPct = if (totalRecords > 0) nullCount.toDouble / totalRecords else 0.0
      
      if (nullPct > threshold) {
        val message = s"Column '$col' has ${nullPct * 100}% null values (threshold: ${threshold * 100}%)"
        logger.warn(message)
        failures += message
      }
    }
    
    ValidationResult(
      passed = failures.isEmpty,
      failures = failures.toList
    )
  }
  
  /**
   * Validate that numeric columns are within expected ranges.
   * 
   * @param df DataFrame to validate
   * @param columnRanges Map of column name to (min, max) tuple
   * @return ValidationResult with details
   */
  def validateRanges(
    df: DataFrame,
    columnRanges: Map[String, (Double, Double)]
  ): ValidationResult = {
    
    val failures = scala.collection.mutable.ListBuffer[String]()
    
    columnRanges.foreach { case (colName, (minVal, maxVal)) =>
      val outOfRangeCount = df.filter(
        df(colName) < minVal || df(colName) > maxVal
      ).count()
      
      if (outOfRangeCount > 0) {
        val message = s"Column '$colName' has $outOfRangeCount values outside range [$minVal, $maxVal]"
        logger.warn(message)
        failures += message
      }
    }
    
    ValidationResult(
      passed = failures.isEmpty,
      failures = failures.toList
    )
  }
  
  /**
   * Validate uniqueness of specified columns.
   * 
   * @param df DataFrame to validate
   * @param columns Columns that should be unique
   * @return ValidationResult with details
   */
  def validateUniqueness(
    df: DataFrame,
    columns: Seq[String]
  ): ValidationResult = {
    
    val totalRecords = df.count()
    val failures = scala.collection.mutable.ListBuffer[String]()
    
    columns.foreach { col =>
      val distinctCount = df.select(col).distinct().count()
      
      if (distinctCount < totalRecords) {
        val duplicates = totalRecords - distinctCount
        val message = s"Column '$col' has $duplicates duplicate values"
        logger.warn(message)
        failures += message
      }
    }
    
    ValidationResult(
      passed = failures.isEmpty,
      failures = failures.toList
    )
  }
  
  /**
   * Validate schema matches expected schema.
   * 
   * @param df DataFrame to validate
   * @param expectedColumns Expected column names
   * @return ValidationResult with details
   */
  def validateSchema(
    df: DataFrame,
    expectedColumns: Seq[String]
  ): ValidationResult = {
    
    val actualColumns = df.columns.toSet
    val expectedSet = expectedColumns.toSet
    
    val missing = expectedSet.diff(actualColumns)
    val extra = actualColumns.diff(expectedSet)
    
    val failures = scala.collection.mutable.ListBuffer[String]()
    
    if (missing.nonEmpty) {
      failures += s"Missing columns: ${missing.mkString(", ")}"
    }
    
    if (extra.nonEmpty) {
      failures += s"Unexpected columns: ${extra.mkString(", ")}"
    }
    
    ValidationResult(
      passed = failures.isEmpty,
      failures = failures.toList
    )
  }
  
  /**
   * Generate comprehensive data quality report.
   * 
   * @param df DataFrame to analyze
   * @return DataQualityReport with statistics
   */
  def generateQualityReport(df: DataFrame): DataQualityReport = {
    
    val totalRecords = df.count()
    val columnStats = df.columns.map { col =>
      val nullCount = df.filter(df(col).isNull).count()
      val distinctCount = df.select(col).distinct().count()
      
      ColumnStats(
        name = col,
        nullCount = nullCount,
        nullPercentage = if (totalRecords > 0) nullCount.toDouble / totalRecords else 0.0,
        distinctCount = distinctCount,
        dataType = df.schema(col).dataType.toString
      )
    }
    
    DataQualityReport(
      totalRecords = totalRecords,
      columnStats = columnStats.toList
    )
  }
}

/**
 * Result of a validation check.
 * 
 * @param passed Whether validation passed
 * @param failures List of failure messages
 */
case class ValidationResult(
  passed: Boolean,
  failures: List[String] = List.empty
) {
  def isSuccess: Boolean = passed
  def isFailure: Boolean = !passed
}

/**
 * Statistics for a single column.
 * 
 * @param name Column name
 * @param nullCount Number of null values
 * @param nullPercentage Percentage of null values
 * @param distinctCount Number of distinct values
 * @param dataType Data type of the column
 */
case class ColumnStats(
  name: String,
  nullCount: Long,
  nullPercentage: Double,
  distinctCount: Long,
  dataType: String
)

/**
 * Comprehensive data quality report.
 * 
 * @param totalRecords Total number of records
 * @param columnStats Statistics for each column
 */
case class DataQualityReport(
  totalRecords: Long,
  columnStats: List[ColumnStats]
)
