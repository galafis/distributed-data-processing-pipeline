package com.gabriellafis.pipeline.core

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.{Logger, LoggerFactory}
import com.typesafe.config.{Config, ConfigFactory}

/**
 * Base trait for all Spark jobs in the pipeline.
 * 
 * Provides common functionality including Spark session management,
 * configuration loading, and logging.
 * 
 * @author Gabriel Demetrios Lafis
 */
trait BaseSparkJob {
  
  protected val logger: Logger = LoggerFactory.getLogger(getClass)
  protected val config: Config = ConfigFactory.load()
  
  /**
   * Get or create SparkSession with appropriate configuration.
   * 
   * @param appName Name of the Spark application
   * @return Configured SparkSession
   */
  protected def getSparkSession(appName: String): SparkSession = {
    logger.info(s"Initializing Spark session for: $appName")
    
    val builder = SparkSession.builder()
      .appName(appName)
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("spark.sql.adaptive.enabled", "true")
      .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    
    // Add master if running locally
    if (config.hasPath("spark.master")) {
      builder.master(config.getString("spark.master"))
    }
    
    val spark = builder.getOrCreate()
    
    logger.info(s"Spark version: ${spark.version}")
    logger.info(s"Spark master: ${spark.sparkContext.master}")
    
    spark
  }
  
  /**
   * Read data from a source.
   * 
   * @param spark SparkSession
   * @param path Path to data source
   * @param format Data format (parquet, delta, csv, json)
   * @param options Additional read options
   * @return DataFrame containing the data
   */
  protected def readData(
    spark: SparkSession,
    path: String,
    format: String = "parquet",
    options: Map[String, String] = Map.empty
  ): DataFrame = {
    logger.info(s"Reading data from: $path (format: $format)")
    
    val reader = spark.read.format(format)
    options.foreach { case (key, value) => reader.option(key, value) }
    
    val df = reader.load(path)
    logger.info(s"Loaded ${df.count()} records")
    
    df
  }
  
  /**
   * Write data to a destination.
   * 
   * @param df DataFrame to write
   * @param path Destination path
   * @param format Output format (parquet, delta, csv, json)
   * @param mode Write mode (overwrite, append, error, ignore)
   * @param partitionBy Columns to partition by
   * @param options Additional write options
   */
  protected def writeData(
    df: DataFrame,
    path: String,
    format: String = "delta",
    mode: String = "overwrite",
    partitionBy: Seq[String] = Seq.empty,
    options: Map[String, String] = Map.empty
  ): Unit = {
    logger.info(s"Writing data to: $path (format: $format, mode: $mode)")
    
    val writer = df.write.format(format).mode(mode)
    
    if (partitionBy.nonEmpty) {
      writer.partitionBy(partitionBy: _*)
    }
    
    options.foreach { case (key, value) => writer.option(key, value) }
    
    writer.save(path)
    logger.info("Data written successfully")
  }
  
  /**
   * Execute the job.
   * 
   * @param args Command line arguments
   */
  def run(args: Array[String]): Unit
  
  /**
   * Main entry point for the job.
   * 
   * @param args Command line arguments
   */
  def main(args: Array[String]): Unit = {
    try {
      logger.info(s"Starting job: ${getClass.getSimpleName}")
      run(args)
      logger.info("Job completed successfully")
    } catch {
      case e: Exception =>
        logger.error("Job failed with exception", e)
        throw e
    }
  }
}

