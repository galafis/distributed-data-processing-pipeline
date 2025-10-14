package com.gabriellafis.pipeline.core

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConverters._

/**
 * Configuration manager for pipeline settings.
 * 
 * Centralizes access to application configuration from application.conf
 * 
 * @author Gabriel Demetrios Lafis
 */
object ConfigManager {
  
  private val config: Config = ConfigFactory.load()
  
  /**
   * Get a string configuration value.
   * 
   * @param path Configuration path
   * @param default Default value if not found
   * @return Configuration value
   */
  def getString(path: String, default: String = ""): String = {
    if (config.hasPath(path)) config.getString(path)
    else default
  }
  
  /**
   * Get an integer configuration value.
   * 
   * @param path Configuration path
   * @param default Default value if not found
   * @return Configuration value
   */
  def getInt(path: String, default: Int = 0): Int = {
    if (config.hasPath(path)) config.getInt(path)
    else default
  }
  
  /**
   * Get a boolean configuration value.
   * 
   * @param path Configuration path
   * @param default Default value if not found
   * @return Configuration value
   */
  def getBoolean(path: String, default: Boolean = false): Boolean = {
    if (config.hasPath(path)) config.getBoolean(path)
    else default
  }
  
  /**
   * Get a double configuration value.
   * 
   * @param path Configuration path
   * @param default Default value if not found
   * @return Configuration value
   */
  def getDouble(path: String, default: Double = 0.0): Double = {
    if (config.hasPath(path)) config.getDouble(path)
    else default
  }
  
  /**
   * Get a list of strings configuration value.
   * 
   * @param path Configuration path
   * @return List of configuration values
   */
  def getStringList(path: String): List[String] = {
    if (config.hasPath(path)) 
      config.getStringList(path).asScala.toList
    else 
      List.empty
  }
  
  /**
   * Get Spark configuration as a Map.
   * 
   * @return Map of Spark configuration
   */
  def getSparkConfig: Map[String, String] = {
    Map(
      "spark.app.name" -> getString("spark.app_name", "DataPipeline"),
      "spark.master" -> getString("spark.master", "local[*]"),
      "spark.executor.memory" -> getString("spark.executor.memory", "4g"),
      "spark.executor.cores" -> getInt("spark.executor.cores", 2).toString,
      "spark.driver.memory" -> getString("spark.driver.memory", "2g")
    )
  }
  
  /**
   * Get pipeline input path.
   * 
   * @return Input path
   */
  def getInputPath: String = getString("pipeline.input.path", "data/raw")
  
  /**
   * Get pipeline output path.
   * 
   * @return Output path
   */
  def getOutputPath: String = getString("pipeline.output.path", "data/processed")
  
  /**
   * Get data quality threshold for null values.
   * 
   * @return Null threshold (0.0 to 1.0)
   */
  def getNullThreshold: Double = getDouble("quality.null_threshold", 0.05)
  
  /**
   * Get data quality threshold for duplicate values.
   * 
   * @return Duplicate threshold (0.0 to 1.0)
   */
  def getDuplicateThreshold: Double = getDouble("quality.duplicate_threshold", 0.01)
}
