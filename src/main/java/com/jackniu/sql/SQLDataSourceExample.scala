package com.jackniu.sql

import org.apache.spark.sql.SparkSession

/**
  * Created by JackNiu on 2017/7/7.
  */
object SQLDataSourceExample {
  case class Person(name: String, age: Long)



  def main(args: Array[String]) {
    System.setProperty("hadoop.home.dir", "D:\\software\\hadoop")
    val spark = SparkSession
      .builder()
      .appName("Spark SQL Example")
      .master("local")
      .config("spark.sql.warehouse.dir", "data/spark-warehouse")
      .getOrCreate()
    import spark.implicits._

//    runBasicDataSourceExample(spark)
    runParquetSchemaMergingExample(spark)
//    runBasicParquetExample(spark)
//    runParquetSchemaMergingExample(spark)
//    runJsonDatasetExample(spark)

    spark.stop()
  }
  def runBasicDataSourceExample(spark: SparkSession):Unit={
    val usersDF = spark.read.load("src/main/resources/users.parquet") // DataFrame
    usersDF.select("name", "favorite_color").write.save("namesAndFavColors.parquet")

    val peopleDF = spark.read.format("json").load("src/main/resources/people.json")
    peopleDF.select("name", "age").write.format("parquet").save("namesAndAges.parquet")

    // 直接在parquet上运行
    val sqlDF = spark.sql("SELECT * FROM parquet.`src/main/resources/users.parquet`")
    sqlDF.show()
  }

  private def runBasicParquetExample(spark: SparkSession): Unit = {
    // Encoders for most common types are automatically provided by importing spark.implicits._
    import spark.implicits._

    val peopleDF = spark.read.json("src/main/resources/people.json")
    // DataFrames can be saved as Parquet files, maintaining the schema information
    peopleDF.write.parquet("people.parquet")

    val parquetFileDF = spark.read.parquet("people.parquet")

    // Parquet files can also be used to create a temporary view and then used in SQL statements
    parquetFileDF.createOrReplaceTempView("parquetFile")
    val namesDF = spark.sql("SELECT name FROM parquetFile WHERE age BETWEEN 13 AND 19")
    namesDF.map(attributes => "Name: " + attributes(0)).show()
    // +------------+
    // |       value|
    // +------------+
    // |Name: Justin|
    // +------------+
    // $example off:basic_parquet_example$
  }

  private def runParquetSchemaMergingExample(spark: SparkSession): Unit = {
    // This is used to implicitly convert an RDD to a DataFrame.
    import spark.implicits._

    // Create a simple DataFrame, store into a partition directory
    val squaresDF = spark.sparkContext.makeRDD(1 to 5).map(i => (i, i * i)).toDF("value", "square")
    squaresDF.write.parquet("data/test_table/key=1")

    // Create another DataFrame in a new partition directory,
    // adding a new column and dropping an existing column
    val cubesDF = spark.sparkContext.makeRDD(6 to 10).map(i => (i, i * i * i)).toDF("value", "cube")
    cubesDF.write.parquet("data/test_table/key=2")

    // Read the partitioned table
    val mergedDF = spark.read.option("mergeSchema", "true").parquet("data/test_table")
    mergedDF.printSchema()

    // The final schema consists of all 3 columns in the Parquet files together
    // with the partitioning column appeared in the partition directory paths
    // root
    // |-- value: int (nullable = true)
    // |-- square: int (nullable = true)
    // |-- cube: int (nullable = true)
    // |-- key : int (nullable = true)
    // $example off:schema_merging$
  }

}
