package com.gabriellafis.pipeline.jobs

import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Test suite for BatchETLJob.
 * 
 * @author Gabriel Demetrios Lafis
 */
class BatchETLJobSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  
  var spark: SparkSession = _
  
  override def beforeAll(): Unit = {
    spark = SparkSession.builder()
      .appName("BatchETLJobSpec")
      .master("local[*]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("spark.ui.enabled", "false")
      .getOrCreate()
    
    spark.sparkContext.setLogLevel("WARN")
  }
  
  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
  }
  
  "BatchETLJob" should "process transaction records correctly" in {
    import spark.implicits._
    
    // Create test data
    val testData = Seq(
      ("tx1", "cust1", "prod1", 100.0, 2, 1609459200L, "US", "Electronics"),
      ("tx2", "cust2", "prod2", 50.0, 1, 1609459200L, "UK", "Books"),
      ("tx3", "cust1", "prod3", 75.0, 3, 1609459200L, "US", "Clothing")
    ).toDF("transactionId", "customerId", "productId", "amount", "quantity", "timestamp", "country", "category")
    
    val inputPath = "/tmp/test-batch-etl-input"
    val outputPath = "/tmp/test-batch-etl-output"
    
    // Write test data
    testData.write.mode("overwrite").parquet(inputPath)
    
    // Run ETL job
    BatchETLJob.run(Array(inputPath, outputPath))
    
    // Verify output
    val detailDf = spark.read.format("delta").load(s"$outputPath/transactions_detail")
    val aggregateDf = spark.read.format("delta").load(s"$outputPath/transactions_aggregate")
    
    // Check detail data
    detailDf.count() shouldBe 3
    detailDf.columns should contain allOf("transactionId", "customerId", "total_amount", "date")
    
    // Check aggregate data
    aggregateDf.count() should be > 0L
    aggregateDf.columns should contain allOf("country", "category", "transaction_count", "total_revenue")
  }
  
  it should "remove duplicate transactions" in {
    import spark.implicits._
    
    // Create test data with duplicates
    val testData = Seq(
      ("tx1", "cust1", "prod1", 100.0, 2, 1609459200L, "US", "Electronics"),
      ("tx1", "cust1", "prod1", 100.0, 2, 1609459200L, "US", "Electronics"), // duplicate
      ("tx2", "cust2", "prod2", 50.0, 1, 1609459200L, "UK", "Books")
    ).toDF("transactionId", "customerId", "productId", "amount", "quantity", "timestamp", "country", "category")
    
    val inputPath = "/tmp/test-batch-etl-dedup-input"
    val outputPath = "/tmp/test-batch-etl-dedup-output"
    
    // Write test data
    testData.write.mode("overwrite").parquet(inputPath)
    
    // Run ETL job
    BatchETLJob.run(Array(inputPath, outputPath))
    
    // Verify deduplication
    val detailDf = spark.read.format("delta").load(s"$outputPath/transactions_detail")
    detailDf.count() shouldBe 2
  }
  
  it should "filter out invalid records" in {
    import spark.implicits._
    
    // Create test data with invalid records
    val testData = Seq(
      ("tx1", "cust1", "prod1", 100.0, 2, 1609459200L, "US", "Electronics"),
      (null, "cust2", "prod2", 50.0, 1, 1609459200L, "UK", "Books"), // null transaction ID
      ("tx3", "cust3", "prod3", -75.0, 3, 1609459200L, "US", "Clothing"), // negative amount
      ("tx4", "cust4", "prod4", 200.0, 0, 1609459200L, "CA", "Food") // zero quantity
    ).toDF("transactionId", "customerId", "productId", "amount", "quantity", "timestamp", "country", "category")
    
    val inputPath = "/tmp/test-batch-etl-invalid-input"
    val outputPath = "/tmp/test-batch-etl-invalid-output"
    
    // Write test data
    testData.write.mode("overwrite").parquet(inputPath)
    
    // Run ETL job
    BatchETLJob.run(Array(inputPath, outputPath))
    
    // Verify only valid records remain
    val detailDf = spark.read.format("delta").load(s"$outputPath/transactions_detail")
    detailDf.count() shouldBe 1
  }
  
  it should "calculate total_amount correctly" in {
    import spark.implicits._
    
    val testData = Seq(
      ("tx1", "cust1", "prod1", 10.0, 5, 1609459200L, "US", "Electronics")
    ).toDF("transactionId", "customerId", "productId", "amount", "quantity", "timestamp", "country", "category")
    
    val inputPath = "/tmp/test-batch-etl-calc-input"
    val outputPath = "/tmp/test-batch-etl-calc-output"
    
    testData.write.mode("overwrite").parquet(inputPath)
    
    BatchETLJob.run(Array(inputPath, outputPath))
    
    val detailDf = spark.read.format("delta").load(s"$outputPath/transactions_detail")
    val totalAmount = detailDf.select("total_amount").first().getDouble(0)
    
    totalAmount shouldBe 50.0
  }
}
