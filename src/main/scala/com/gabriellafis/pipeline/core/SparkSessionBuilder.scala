package com.gabriellafis.pipeline.core

import org.apache.spark.sql.SparkSession
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Spark session builder with configuration management.
 * 
 * Provides centralized configuration for all Spark jobs.
 * 
 * @author Gabriel Demetrios Lafis
 */
object SparkSessionBuilder {
  
  /**
   * Create a SparkSession with standard configuration.
   * 
   * @param appName Application name
   * @param master Spark master URL (optional, uses config if not provided)
   * @param additionalConfig Additional Spark configuration
   * @return Configured SparkSession
   */
  def buildSession(
    appName: String,
    master: Option[String] = None,
    additionalConfig: Map[String, String] = Map.empty
  ): SparkSession = {
    
    val config: Config = ConfigFactory.load()
    
    val builder = SparkSession.builder()
      .appName(appName)
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    
    // Set master from parameter or config
    master.orElse {
      if (config.hasPath("spark.master")) Some(config.getString("spark.master"))
      else None
    }.foreach(builder.master)
    
    // Add any additional configuration
    additionalConfig.foreach { case (key, value) =>
      builder.config(key, value)
    }
    
    builder.getOrCreate()
  }
  
  /**
   * Create a SparkSession for testing.
   * 
   * @param appName Application name
   * @return SparkSession configured for testing
   */
  def buildTestSession(appName: String): SparkSession = {
    buildSession(
      appName = appName,
      master = Some("local[*]"),
      additionalConfig = Map(
        "spark.ui.enabled" -> "false",
        "spark.sql.shuffle.partitions" -> "4"
      )
    )
  }
}
