package com.gabriellafis.pipeline.transformations

import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Data cleansing transformations.
 * 
 * Provides common data cleansing operations.
 * 
 * @author Gabriel Demetrios Lafis
 */
object Cleansing {
  
  /**
   * Remove null values from specified columns.
   * 
   * @param df Input DataFrame
   * @param columns Columns to check for nulls
   * @return DataFrame with nulls removed
   */
  def removeNulls(df: DataFrame, columns: String*): DataFrame = {
    columns.foldLeft(df) { (acc, col) =>
      acc.filter(acc(col).isNotNull)
    }
  }
  
  /**
   * Remove duplicate rows based on specified columns.
   * 
   * @param df Input DataFrame
   * @param columns Columns to use for deduplication
   * @return DataFrame with duplicates removed
   */
  def removeDuplicates(df: DataFrame, columns: String*): DataFrame = {
    if (columns.isEmpty) df.dropDuplicates()
    else df.dropDuplicates(columns)
  }
  
  /**
   * Trim whitespace from string columns.
   * 
   * @param df Input DataFrame
   * @param columns Columns to trim (if empty, trims all string columns)
   * @return DataFrame with trimmed strings
   */
  def trimStrings(df: DataFrame, columns: String*): DataFrame = {
    val colsToTrim = if (columns.isEmpty) {
      df.schema.fields
        .filter(_.dataType == StringType)
        .map(_.name)
    } else {
      columns
    }
    
    colsToTrim.foldLeft(df) { (acc, col) =>
      acc.withColumn(col, trim(acc(col)))
    }
  }
  
  /**
   * Standardize string values to lowercase.
   * 
   * @param df Input DataFrame
   * @param columns Columns to lowercase
   * @return DataFrame with lowercase strings
   */
  def toLowercase(df: DataFrame, columns: String*): DataFrame = {
    columns.foldLeft(df) { (acc, col) =>
      acc.withColumn(col, lower(acc(col)))
    }
  }
  
  /**
   * Standardize string values to uppercase.
   * 
   * @param df Input DataFrame
   * @param columns Columns to uppercase
   * @return DataFrame with uppercase strings
   */
  def toUppercase(df: DataFrame, columns: String*): DataFrame = {
    columns.foldLeft(df) { (acc, col) =>
      acc.withColumn(col, upper(acc(col)))
    }
  }
  
  /**
   * Replace null values with default values.
   * 
   * @param df Input DataFrame
   * @param replacements Map of column name to replacement value
   * @return DataFrame with nulls replaced
   */
  def fillNulls(df: DataFrame, replacements: Map[String, Any]): DataFrame = {
    df.na.fill(replacements)
  }
  
  /**
   * Filter out rows where numeric columns are negative.
   * 
   * @param df Input DataFrame
   * @param columns Numeric columns to check
   * @return DataFrame with only positive values
   */
  def keepPositiveValues(df: DataFrame, columns: String*): DataFrame = {
    columns.foldLeft(df) { (acc, col) =>
      acc.filter(acc(col) > 0)
    }
  }
  
  /**
   * Remove rows with outliers based on standard deviation.
   * 
   * @param df Input DataFrame
   * @param column Column to check for outliers
   * @param stdDevThreshold Number of standard deviations for outlier detection
   * @return DataFrame with outliers removed
   */
  def removeOutliers(
    df: DataFrame,
    column: String,
    stdDevThreshold: Double = 3.0
  ): DataFrame = {
    
    val stats = df.select(
      mean(column).as("mean"),
      stddev(column).as("stddev")
    ).first()
    
    val meanVal = stats.getDouble(0)
    val stddevVal = stats.getDouble(1)
    
    val lowerBound = meanVal - (stdDevThreshold * stddevVal)
    val upperBound = meanVal + (stdDevThreshold * stddevVal)
    
    df.filter(
      col(column) >= lowerBound && col(column) <= upperBound
    )
  }
  
  /**
   * Apply comprehensive cleansing pipeline.
   * 
   * @param df Input DataFrame
   * @param trimColumns Columns to trim
   * @param dedupeColumns Columns for deduplication
   * @return Cleansed DataFrame
   */
  def applyStandardCleansing(
    df: DataFrame,
    trimColumns: Seq[String] = Seq.empty,
    dedupeColumns: Seq[String] = Seq.empty
  ): DataFrame = {
    
    var result = df
    
    // Trim strings
    if (trimColumns.nonEmpty) {
      result = trimStrings(result, trimColumns: _*)
    }
    
    // Remove duplicates
    if (dedupeColumns.nonEmpty) {
      result = removeDuplicates(result, dedupeColumns: _*)
    }
    
    result
  }
}
