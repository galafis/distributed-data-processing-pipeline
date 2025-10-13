package com.gabriellafis.pipeline.jobs

import com.gabriellafis.pipeline.core.BaseSparkJob
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.types._

/**
 * Streaming job for real-time data processing.
 * 
 * This job demonstrates:
 * - Structured streaming
 * - Windowed aggregations
 * - Stateful processing
 * - Delta Lake streaming sink
 * 
 * @author Gabriel Demetrios Lafis
 */
object StreamingJob extends BaseSparkJob {
  
  /**
   * Define schema for incoming stream data.
   */
  private val streamSchema = StructType(Seq(
    StructField("eventId", StringType, nullable = false),
    StructField("userId", StringType, nullable = false),
    StructField("eventType", StringType, nullable = false),
    StructField("value", DoubleType, nullable = true),
    StructField("timestamp", LongType, nullable = false),
    StructField("metadata", StringType, nullable = true)
  ))
  
  /**
   * Read streaming data from source.
   */
  private def readStream(spark: SparkSession, sourcePath: String): DataFrame = {
    logger.info(s"Reading stream from: $sourcePath")
    
    spark.readStream
      .format("json")
      .schema(streamSchema)
      .option("maxFilesPerTrigger", "10")
      .load(sourcePath)
  }
  
  /**
   * Transform streaming data.
   */
  private def transformStream(df: DataFrame): DataFrame = {
    logger.info("Applying stream transformations")
    
    df
      .withColumn("event_time", to_timestamp(from_unixtime(col("timestamp"))))
      .withColumn("date", to_date(col("event_time")))
      .withWatermark("event_time", "10 minutes")
      .filter(col("value").isNotNull)
      .filter(col("value") > 0)
  }
  
  /**
   * Perform windowed aggregations on stream.
   */
  private def aggregateStream(df: DataFrame): DataFrame = {
    logger.info("Creating windowed aggregations")
    
    df.groupBy(
      window(col("event_time"), "5 minutes", "1 minute"),
      col("eventType"),
      col("userId")
    ).agg(
      count("*").as("event_count"),
      sum("value").as("total_value"),
      avg("value").as("avg_value"),
      min("value").as("min_value"),
      max("value").as("max_value")
    )
    .select(
      col("window.start").as("window_start"),
      col("window.end").as("window_end"),
      col("eventType"),
      col("userId"),
      col("event_count"),
      col("total_value"),
      col("avg_value"),
      col("min_value"),
      col("max_value")
    )
  }
  
  /**
   * Detect anomalies in streaming data.
   */
  private def detectAnomalies(df: DataFrame): DataFrame = {
    logger.info("Detecting anomalies")
    
    // Calculate statistics per user and event type
    val stats = df.groupBy("userId", "eventType")
      .agg(
        avg("value").as("mean_value"),
        stddev("value").as("stddev_value")
      )
    
    // Join and flag anomalies (values > 3 standard deviations)
    df.join(stats, Seq("userId", "eventType"), "left")
      .withColumn(
        "is_anomaly",
        when(
          col("value") > col("mean_value") + (3 * col("stddev_value")) ||
          col("value") < col("mean_value") - (3 * col("stddev_value")),
          lit(true)
        ).otherwise(lit(false))
      )
      .filter(col("is_anomaly") === true)
      .select("eventId", "userId", "eventType", "value", "event_time", "is_anomaly")
  }
  
  /**
   * Write stream to Delta Lake.
   */
  private def writeStream(
    df: DataFrame,
    outputPath: String,
    checkpointPath: String,
    queryName: String
  ): Unit = {
    logger.info(s"Writing stream to: $outputPath")
    
    df.writeStream
      .format("delta")
      .outputMode(OutputMode.Append())
      .option("checkpointLocation", checkpointPath)
      .option("path", outputPath)
      .queryName(queryName)
      .trigger(Trigger.ProcessingTime("30 seconds"))
      .start()
  }
  
  /**
   * Run the streaming job.
   */
  override def run(args: Array[String]): Unit = {
    val sourcePath = if (args.length > 0) args(0) else config.getString("pipeline.stream.input.path")
    val outputPath = if (args.length > 1) args(1) else config.getString("pipeline.stream.output.path")
    val checkpointPath = s"$outputPath/checkpoints"
    
    val spark = getSparkSession("StreamingJob")
    
    try {
      // Read stream
      val rawStream = readStream(spark, sourcePath)
      
      // Transform stream
      val transformedStream = transformStream(rawStream)
      
      // Create aggregations
      val aggregatedStream = aggregateStream(transformedStream)
      
      // Detect anomalies
      val anomalies = detectAnomalies(transformedStream)
      
      // Write streams
      writeStream(
        transformedStream,
        s"$outputPath/events_detail",
        s"$checkpointPath/events_detail",
        "events_detail_stream"
      )
      
      writeStream(
        aggregatedStream,
        s"$outputPath/events_aggregate",
        s"$checkpointPath/events_aggregate",
        "events_aggregate_stream"
      )
      
      writeStream(
        anomalies,
        s"$outputPath/anomalies",
        s"$checkpointPath/anomalies",
        "anomalies_stream"
      )
      
      // Wait for termination
      logger.info("Streaming job started. Waiting for termination...")
      spark.streams.awaitAnyTermination()
      
    } finally {
      spark.stop()
    }
  }
}

