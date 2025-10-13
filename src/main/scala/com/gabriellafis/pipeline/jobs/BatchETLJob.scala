package com.gabriellafis.pipeline.jobs

import com.gabriellafis.pipeline.core.BaseSparkJob
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Batch ETL job for processing large-scale data.
 * 
 * This job demonstrates:
 * - Reading from multiple sources
 * - Complex transformations
 * - Data quality checks
 * - Writing to Delta Lake
 * - Partitioning strategies
 * 
 * @author Gabriel Demetrios Lafis
 */
object BatchETLJob extends BaseSparkJob {
  
  case class TransactionRecord(
    transactionId: String,
    customerId: String,
    productId: String,
    amount: Double,
    quantity: Int,
    timestamp: Long,
    country: String,
    category: String
  )
  
  /**
   * Extract data from source.
   */
  private def extract(spark: SparkSession, inputPath: String): DataFrame = {
    logger.info("Extracting data from source")
    
    readData(
      spark,
      inputPath,
      format = "parquet",
      options = Map(
        "mergeSchema" -> "true",
        "recursiveFileLookup" -> "true"
      )
    )
  }
  
  /**
   * Transform data with business logic.
   */
  private def transform(df: DataFrame): DataFrame = {
    logger.info("Applying transformations")
    
    df
      // Add derived columns
      .withColumn("total_amount", col("amount") * col("quantity"))
      .withColumn("date", to_date(from_unixtime(col("timestamp"))))
      .withColumn("year", year(col("date")))
      .withColumn("month", month(col("date")))
      .withColumn("day", dayofmonth(col("date")))
      
      // Data quality: remove nulls and invalid records
      .filter(col("transactionId").isNotNull)
      .filter(col("amount") > 0)
      .filter(col("quantity") > 0)
      
      // Add metadata
      .withColumn("processed_at", current_timestamp())
      .withColumn("data_version", lit("1.0"))
      
      // Deduplication
      .dropDuplicates("transactionId")
  }
  
  /**
   * Enrich data with additional information.
   */
  private def enrich(df: DataFrame, spark: SparkSession): DataFrame = {
    logger.info("Enriching data")
    
    // Create customer aggregations
    val customerStats = df.groupBy("customerId")
      .agg(
        count("*").as("total_transactions"),
        sum("total_amount").as("lifetime_value"),
        avg("total_amount").as("avg_transaction_value"),
        max("timestamp").as("last_transaction_timestamp")
      )
    
    // Join back to main dataframe
    df.join(
      customerStats,
      Seq("customerId"),
      "left"
    )
  }
  
  /**
   * Aggregate data for analytics.
   */
  private def aggregate(df: DataFrame): DataFrame = {
    logger.info("Creating aggregations")
    
    df.groupBy("country", "category", "year", "month")
      .agg(
        count("*").as("transaction_count"),
        sum("total_amount").as("total_revenue"),
        avg("total_amount").as("avg_revenue"),
        countDistinct("customerId").as("unique_customers"),
        countDistinct("productId").as("unique_products")
      )
      .orderBy(desc("total_revenue"))
  }
  
  /**
   * Load data to destination.
   */
  private def load(
    detailDf: DataFrame,
    aggregateDf: DataFrame,
    outputPath: String
  ): Unit = {
    logger.info("Loading data to destination")
    
    // Write detailed data partitioned by year and month
    writeData(
      detailDf,
      s"$outputPath/transactions_detail",
      format = "delta",
      mode = "overwrite",
      partitionBy = Seq("year", "month")
    )
    
    // Write aggregated data
    writeData(
      aggregateDf,
      s"$outputPath/transactions_aggregate",
      format = "delta",
      mode = "overwrite",
      partitionBy = Seq("country", "year", "month")
    )
  }
  
  /**
   * Data quality checks.
   */
  private def qualityChecks(df: DataFrame): Unit = {
    logger.info("Running data quality checks")
    
    val totalRecords = df.count()
    val nullCustomers = df.filter(col("customerId").isNull).count()
    val negativeAmounts = df.filter(col("amount") < 0).count()
    val futureTransactions = df.filter(col("timestamp") > System.currentTimeMillis() / 1000).count()
    
    logger.info(s"Total records: $totalRecords")
    logger.info(s"Null customers: $nullCustomers")
    logger.info(s"Negative amounts: $negativeAmounts")
    logger.info(s"Future transactions: $futureTransactions")
    
    // Assert quality thresholds
    require(nullCustomers == 0, "Found null customer IDs")
    require(negativeAmounts == 0, "Found negative amounts")
    require(futureTransactions == 0, "Found future transactions")
  }
  
  /**
   * Run the ETL job.
   */
  override def run(args: Array[String]): Unit = {
    val inputPath = if (args.length > 0) args(0) else config.getString("pipeline.input.path")
    val outputPath = if (args.length > 1) args(1) else config.getString("pipeline.output.path")
    
    val spark = getSparkSession("BatchETLJob")
    
    try {
      // ETL Pipeline
      val rawData = extract(spark, inputPath)
      val transformedData = transform(rawData)
      val enrichedData = enrich(transformedData, spark)
      
      // Quality checks
      qualityChecks(enrichedData)
      
      // Aggregations
      val aggregatedData = aggregate(enrichedData)
      
      // Load to destination
      load(enrichedData, aggregatedData, outputPath)
      
      // Show statistics
      logger.info(s"Processed ${enrichedData.count()} records")
      logger.info(s"Created ${aggregatedData.count()} aggregate records")
      
    } finally {
      spark.stop()
    }
  }
}

