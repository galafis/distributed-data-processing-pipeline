package com.gabriellafis.pipeline.utils

import org.apache.spark.sql.SparkSession
import org.slf4j.{Logger, LoggerFactory}
import scala.collection.mutable

/**
 * Metrics collector for monitoring pipeline execution.
 * 
 * Collects and reports performance metrics during job execution.
 * 
 * @author Gabriel Demetrios Lafis
 */
class MetricsCollector(spark: SparkSession) {
  
  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val metrics = mutable.Map[String, Any]()
  private val timings = mutable.Map[String, Long]()
  
  /**
   * Record a metric value.
   * 
   * @param name Metric name
   * @param value Metric value
   */
  def recordMetric(name: String, value: Any): Unit = {
    metrics(name) = value
    logger.info(s"Metric recorded: $name = $value")
  }
  
  /**
   * Start timing an operation.
   * 
   * @param operation Operation name
   */
  def startTiming(operation: String): Unit = {
    timings(s"${operation}_start") = System.currentTimeMillis()
    logger.info(s"Started timing: $operation")
  }
  
  /**
   * End timing an operation and record duration.
   * 
   * @param operation Operation name
   */
  def endTiming(operation: String): Long = {
    val startKey = s"${operation}_start"
    val endTime = System.currentTimeMillis()
    
    timings.get(startKey) match {
      case Some(startTime) =>
        val duration = endTime - startTime
        metrics(s"${operation}_duration_ms") = duration
        logger.info(s"Operation '$operation' completed in ${duration}ms")
        duration
      case None =>
        logger.warn(s"No start time found for operation: $operation")
        0L
    }
  }
  
  /**
   * Time an operation and record its duration.
   * 
   * @param operation Operation name
   * @param block Code block to time
   * @tparam T Return type of the block
   * @return Result of the block
   */
  def time[T](operation: String)(block: => T): T = {
    startTiming(operation)
    try {
      block
    } finally {
      endTiming(operation)
    }
  }
  
  /**
   * Get all collected metrics.
   * 
   * @return Map of metric name to value
   */
  def getMetrics: Map[String, Any] = metrics.toMap
  
  /**
   * Print all metrics to console.
   */
  def printMetrics(): Unit = {
    logger.info("=== Pipeline Metrics ===")
    metrics.toSeq.sortBy(_._1).foreach { case (name, value) =>
      logger.info(f"$name%-50s: $value")
    }
    logger.info("========================")
  }
  
  /**
   * Get Spark metrics (records read, written, etc.).
   * 
   * @return Map of Spark metrics
   */
  def getSparkMetrics: Map[String, Long] = {
    try {
      val sc = spark.sparkContext
      Map(
        "records_read" -> sc.statusTracker.getExecutorInfos.map(_.totalInputBytes()).sum,
        "records_written" -> sc.statusTracker.getExecutorInfos.map(_.totalShuffleWrite()).sum,
        "tasks_completed" -> sc.statusTracker.getStageInfo(0).map(_.numCompletedTasks).getOrElse(0L)
      )
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to get Spark metrics: ${e.getMessage}")
        Map.empty
    }
  }
  
  /**
   * Reset all metrics.
   */
  def reset(): Unit = {
    metrics.clear()
    timings.clear()
    logger.info("Metrics reset")
  }
}

/**
 * Companion object for MetricsCollector.
 */
object MetricsCollector {
  
  /**
   * Create a new MetricsCollector instance.
   * 
   * @param spark SparkSession
   * @return MetricsCollector instance
   */
  def apply(spark: SparkSession): MetricsCollector = new MetricsCollector(spark)
}
