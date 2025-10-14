package com.gabriellafis.pipeline.core

import org.apache.spark.sql.SparkSession
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

/**
 * Test suite for BaseSparkJob trait.
 * 
 * @author Gabriel Demetrios Lafis
 */
class BaseSparkJobSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {
  
  var spark: SparkSession = _
  
  override def beforeAll(): Unit = {
    spark = SparkSession.builder()
      .appName("BaseSparkJobSpec")
      .master("local[*]")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()
    
    spark.sparkContext.setLogLevel("WARN")
  }
  
  override def afterAll(): Unit = {
    if (spark != null) {
      spark.stop()
    }
  }
  
  object TestJob extends BaseSparkJob {
    override def run(args: Array[String]): Unit = {
      // Test implementation
    }
  }
  
  "BaseSparkJob" should "create SparkSession with correct configuration" in {
    val session = TestJob.getSparkSession("TestApp")
    
    session should not be null
    session.conf.get("spark.sql.extensions") should include("DeltaSparkSessionExtension")
    session.conf.get("spark.sql.adaptive.enabled") shouldBe "true"
    
    session.stop()
  }
  
  it should "read data from parquet format" in {
    import spark.implicits._
    
    // Create test data
    val testData = Seq(
      ("1", "Alice", 100.0),
      ("2", "Bob", 200.0),
      ("3", "Charlie", 300.0)
    ).toDF("id", "name", "amount")
    
    val testPath = "/tmp/test-parquet-data"
    testData.write.mode("overwrite").parquet(testPath)
    
    // Test reading
    val df = TestJob.readData(spark, testPath, "parquet")
    
    df.count() shouldBe 3
    df.columns should contain allOf("id", "name", "amount")
  }
  
  it should "write data to delta format" in {
    import spark.implicits._
    
    // Create test data
    val testData = Seq(
      ("1", "Alice", 100.0),
      ("2", "Bob", 200.0)
    ).toDF("id", "name", "amount")
    
    val testPath = "/tmp/test-delta-data"
    
    // Test writing
    TestJob.writeData(testData, testPath, "delta", "overwrite")
    
    // Verify data was written
    val df = spark.read.format("delta").load(testPath)
    df.count() shouldBe 2
  }
  
  it should "write data with partitioning" in {
    import spark.implicits._
    
    // Create test data
    val testData = Seq(
      ("1", "Alice", 100.0, "US"),
      ("2", "Bob", 200.0, "UK"),
      ("3", "Charlie", 300.0, "US")
    ).toDF("id", "name", "amount", "country")
    
    val testPath = "/tmp/test-partitioned-data"
    
    // Test writing with partitioning
    TestJob.writeData(testData, testPath, "delta", "overwrite", Seq("country"))
    
    // Verify partitioning
    val df = spark.read.format("delta").load(testPath)
    df.count() shouldBe 3
    
    val partitions = spark.read.format("delta").load(testPath).select("country").distinct().count()
    partitions shouldBe 2
  }
}
