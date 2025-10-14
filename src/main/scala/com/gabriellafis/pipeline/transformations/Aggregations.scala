package com.gabriellafis.pipeline.transformations

import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.functions._

/**
 * Data aggregation transformations.
 * 
 * Provides common aggregation operations.
 * 
 * @author Gabriel Demetrios Lafis
 */
object Aggregations {
  
  /**
   * Aggregate by time window.
   * 
   * @param df Input DataFrame
   * @param timestampColumn Timestamp column for windowing
   * @param windowSize Window size (e.g., "1 hour", "1 day")
   * @param groupByColumns Additional columns to group by
   * @param aggregations Map of column to aggregation function
   * @return Aggregated DataFrame
   */
  def aggregateByTimeWindow(
    df: DataFrame,
    timestampColumn: String,
    windowSize: String,
    groupByColumns: Seq[String] = Seq.empty,
    aggregations: Map[String, String] = Map("*" -> "count")
  ): DataFrame = {
    
    val windowCol = window(col(timestampColumn), windowSize)
    val groupCols = windowCol +: groupByColumns.map(col)
    
    val aggExprs = aggregations.map { case (colName, aggFunc) =>
      aggFunc.toLowerCase match {
        case "count" => count(colName).as(s"${colName}_count")
        case "sum" => sum(colName).as(s"${colName}_sum")
        case "avg" | "mean" => avg(colName).as(s"${colName}_avg")
        case "min" => min(colName).as(s"${colName}_min")
        case "max" => max(colName).as(s"${colName}_max")
        case "stddev" => stddev(colName).as(s"${colName}_stddev")
        case _ => count(colName).as(s"${colName}_count")
      }
    }.toSeq
    
    df.groupBy(groupCols: _*)
      .agg(aggExprs.head, aggExprs.tail: _*)
  }
  
  /**
   * Create daily summary aggregations.
   * 
   * @param df Input DataFrame
   * @param dateColumn Date column
   * @param groupByColumns Columns to group by
   * @param metricsColumns Columns to aggregate
   * @return Daily summary DataFrame
   */
  def createDailySummary(
    df: DataFrame,
    dateColumn: String,
    groupByColumns: Seq[String],
    metricsColumns: Seq[String]
  ): DataFrame = {
    
    val groupCols = (dateColumn +: groupByColumns).map(col)
    
    val aggExprs = metricsColumns.flatMap { colName =>
      Seq(
        sum(colName).as(s"${colName}_sum"),
        avg(colName).as(s"${colName}_avg"),
        min(colName).as(s"${colName}_min"),
        max(colName).as(s"${colName}_max"),
        count(colName).as(s"${colName}_count")
      )
    }
    
    df.groupBy(groupCols: _*)
      .agg(aggExprs.head, aggExprs.tail: _*)
      .orderBy(col(dateColumn).desc)
  }
  
  /**
   * Calculate percentiles for numeric columns.
   * 
   * @param df Input DataFrame
   * @param column Column to calculate percentiles for
   * @param percentiles Percentiles to calculate (e.g., Seq(0.25, 0.5, 0.75))
   * @return DataFrame with percentile values
   */
  def calculatePercentiles(
    df: DataFrame,
    column: String,
    percentiles: Seq[Double] = Seq(0.25, 0.5, 0.75, 0.95)
  ): DataFrame = {
    
    val percentileExprs = percentiles.map { p =>
      expr(s"percentile_approx($column, $p)").as(s"p${(p * 100).toInt}")
    }
    
    df.select(percentileExprs: _*)
  }
  
  /**
   * Create pivot table.
   * 
   * @param df Input DataFrame
   * @param pivotColumn Column to pivot on
   * @param groupByColumns Columns to group by
   * @param valueColumn Column to aggregate
   * @param aggFunc Aggregation function
   * @return Pivoted DataFrame
   */
  def createPivotTable(
    df: DataFrame,
    pivotColumn: String,
    groupByColumns: Seq[String],
    valueColumn: String,
    aggFunc: String = "sum"
  ): DataFrame = {
    
    val pivotValues = df.select(pivotColumn)
      .distinct()
      .collect()
      .map(_.get(0))
      .toSeq
    
    val grouped = df.groupBy(groupByColumns.map(col): _*)
      .pivot(pivotColumn, pivotValues)
    
    aggFunc.toLowerCase match {
      case "sum" => grouped.sum(valueColumn)
      case "avg" | "mean" => grouped.avg(valueColumn)
      case "count" => grouped.count()
      case "min" => grouped.min(valueColumn)
      case "max" => grouped.max(valueColumn)
      case _ => grouped.sum(valueColumn)
    }
  }
  
  /**
   * Calculate cumulative statistics.
   * 
   * @param df Input DataFrame
   * @param valueColumn Column to calculate cumulative stats for
   * @param orderColumn Column to order by
   * @param partitionColumns Columns to partition by
   * @return DataFrame with cumulative statistics
   */
  def calculateCumulativeStats(
    df: DataFrame,
    valueColumn: String,
    orderColumn: String,
    partitionColumns: Seq[String] = Seq.empty
  ): DataFrame = {
    
    import org.apache.spark.sql.expressions.Window
    
    val windowSpec = Window
      .partitionBy(partitionColumns.map(col): _*)
      .orderBy(col(orderColumn))
      .rowsBetween(Window.unboundedPreceding, Window.currentRow)
    
    df
      .withColumn(s"${valueColumn}_cumsum", sum(valueColumn).over(windowSpec))
      .withColumn(s"${valueColumn}_cumavg", avg(valueColumn).over(windowSpec))
      .withColumn(s"${valueColumn}_cumcount", count(valueColumn).over(windowSpec))
  }
  
  /**
   * Create summary statistics for all numeric columns.
   * 
   * @param df Input DataFrame
   * @return DataFrame with summary statistics
   */
  def createSummaryStats(df: DataFrame): DataFrame = {
    
    val numericColumns = df.schema.fields
      .filter(f => f.dataType.isInstanceOf[org.apache.spark.sql.types.NumericType])
      .map(_.name)
    
    val statsExprs = numericColumns.flatMap { colName =>
      Seq(
        count(colName).as(s"${colName}_count"),
        sum(colName).as(s"${colName}_sum"),
        avg(colName).as(s"${colName}_avg"),
        min(colName).as(s"${colName}_min"),
        max(colName).as(s"${colName}_max"),
        stddev(colName).as(s"${colName}_stddev")
      )
    }
    
    df.select(statsExprs: _*)
  }
}
