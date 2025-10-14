package com.gabriellafis.pipeline.transformations

import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.functions._

/**
 * Data enrichment transformations.
 * 
 * Provides operations to enrich data with additional information.
 * 
 * @author Gabriel Demetrios Lafis
 */
object Enrichment {
  
  /**
   * Add timestamp columns for processing metadata.
   * 
   * @param df Input DataFrame
   * @return DataFrame with timestamp columns
   */
  def addProcessingTimestamps(df: DataFrame): DataFrame = {
    df
      .withColumn("processed_at", current_timestamp())
      .withColumn("processing_date", current_date())
  }
  
  /**
   * Add unique identifier column.
   * 
   * @param df Input DataFrame
   * @param columnName Name for the ID column
   * @return DataFrame with ID column
   */
  def addUniqueId(df: DataFrame, columnName: String = "row_id"): DataFrame = {
    df.withColumn(columnName, monotonically_increasing_id())
  }
  
  /**
   * Parse date components from timestamp.
   * 
   * @param df Input DataFrame
   * @param timestampColumn Source timestamp column
   * @return DataFrame with date components
   */
  def extractDateComponents(df: DataFrame, timestampColumn: String): DataFrame = {
    df
      .withColumn("year", year(col(timestampColumn)))
      .withColumn("month", month(col(timestampColumn)))
      .withColumn("day", dayofmonth(col(timestampColumn)))
      .withColumn("hour", hour(col(timestampColumn)))
      .withColumn("dayofweek", dayofweek(col(timestampColumn)))
      .withColumn("quarter", quarter(col(timestampColumn)))
  }
  
  /**
   * Calculate running totals for a numeric column.
   * 
   * @param df Input DataFrame
   * @param valueColumn Column to calculate running total for
   * @param partitionColumns Columns to partition by
   * @param orderColumns Columns to order by
   * @return DataFrame with running total
   */
  def addRunningTotal(
    df: DataFrame,
    valueColumn: String,
    partitionColumns: Seq[String],
    orderColumns: Seq[String]
  ): DataFrame = {
    
    import org.apache.spark.sql.expressions.Window
    
    val windowSpec = Window
      .partitionBy(partitionColumns.map(col): _*)
      .orderBy(orderColumns.map(col): _*)
      .rowsBetween(Window.unboundedPreceding, Window.currentRow)
    
    df.withColumn(s"${valueColumn}_running_total", sum(valueColumn).over(windowSpec))
  }
  
  /**
   * Add ranking within groups.
   * 
   * @param df Input DataFrame
   * @param partitionColumns Columns to partition by
   * @param orderColumns Columns to order by
   * @param rankColumn Name for the rank column
   * @return DataFrame with ranking
   */
  def addRanking(
    df: DataFrame,
    partitionColumns: Seq[String],
    orderColumns: Seq[String],
    rankColumn: String = "rank"
  ): DataFrame = {
    
    import org.apache.spark.sql.expressions.Window
    
    val windowSpec = Window
      .partitionBy(partitionColumns.map(col): _*)
      .orderBy(orderColumns.map(col).map(_.desc): _*)
    
    df.withColumn(rankColumn, row_number().over(windowSpec))
  }
  
  /**
   * Calculate moving averages.
   * 
   * @param df Input DataFrame
   * @param valueColumn Column to calculate average for
   * @param partitionColumns Columns to partition by
   * @param orderColumns Columns to order by
   * @param windowSize Number of rows in the window
   * @return DataFrame with moving average
   */
  def addMovingAverage(
    df: DataFrame,
    valueColumn: String,
    partitionColumns: Seq[String],
    orderColumns: Seq[String],
    windowSize: Int = 7
  ): DataFrame = {
    
    import org.apache.spark.sql.expressions.Window
    
    val windowSpec = Window
      .partitionBy(partitionColumns.map(col): _*)
      .orderBy(orderColumns.map(col): _*)
      .rowsBetween(-windowSize + 1, 0)
    
    df.withColumn(
      s"${valueColumn}_ma${windowSize}",
      avg(valueColumn).over(windowSpec)
    )
  }
  
  /**
   * Categorize numeric values into bins.
   * 
   * @param df Input DataFrame
   * @param column Column to categorize
   * @param bins Bin boundaries
   * @param labels Labels for each bin
   * @return DataFrame with category column
   */
  def categorize(
    df: DataFrame,
    column: String,
    bins: Seq[Double],
    labels: Seq[String]
  ): DataFrame = {
    
    require(bins.length == labels.length + 1, "Number of bins must be one more than number of labels")
    
    val binConditions = bins.sliding(2).zip(labels).map { case (Seq(lower, upper), label) =>
      when(col(column) >= lower && col(column) < upper, label)
    }.toSeq
    
    val categorizeExpr = binConditions.reduce(_ otherwise _)
      .otherwise("unknown")
    
    df.withColumn(s"${column}_category", categorizeExpr)
  }
  
  /**
   * Add flag columns based on conditions.
   * 
   * @param df Input DataFrame
   * @param flags Map of flag name to condition
   * @return DataFrame with flag columns
   */
  def addFlags(df: DataFrame, flags: Map[String, Column]): DataFrame = {
    flags.foldLeft(df) { case (acc, (name, condition)) =>
      acc.withColumn(name, condition)
    }
  }
  
  /**
   * Lookup and join with dimension table.
   * 
   * @param df Input DataFrame
   * @param dimensionDf Dimension table
   * @param joinKeys Join key columns
   * @param joinType Type of join (left, inner, etc.)
   * @return Enriched DataFrame
   */
  def enrichWithDimension(
    df: DataFrame,
    dimensionDf: DataFrame,
    joinKeys: Seq[String],
    joinType: String = "left"
  ): DataFrame = {
    df.join(dimensionDf, joinKeys, joinType)
  }
}
