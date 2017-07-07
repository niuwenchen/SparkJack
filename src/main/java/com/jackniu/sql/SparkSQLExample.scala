package com.jackniu.sql

import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types.{StringType, StructField, StructType}

/**
  * Created by JackNiu on 2017/7/7.
  */
object SparkSQLExample {
  case class Person(name: String, age: Long)

  def main(args: Array[String]) {
    // $example on:init_session$
    System.setProperty("hadoop.home.dir", "D:\\software\\hadoop")
    val spark = SparkSession
      .builder()
      .appName("Spark SQL Example")
      .master("local")
      .config("spark.sql.warehouse.dir", "data/spark-warehouse")
      .getOrCreate()
    import spark.implicits._
    runProgrammaticSchemaExample(spark)
  }
  private def runBasicDataFrameExample(spark:SparkSession):Unit={
    // 读取Json数据，输出为DataFrame
    val df = spark.read.json("src/main/resources/people.json")
    df.show()
    df.printSchema()
    // Select only the "name" column
    df.select("name").show()
    import spark.implicits._
    // Select everybody, but increment the age by 1
    df.select("name", "age" + 1).show()

    // Select people older than 21
    df.filter($"age" > 21).show()

    // Count people by age
    df.groupBy("age").count().show()

    // Register the DataFrame as a SQL temporary view
    df.createOrReplaceTempView("people")

    val sqlDF = spark.sql("SELECT * FROM people")
    sqlDF.show()
  }

  //Creating Datasets
  private def runDatasetCreationExample(spark: SparkSession): Unit = {
    import spark.implicits._
    val caseClassDS = Seq(Person("Jack",12)).toDS()
    caseClassDS.show()

    val primitiveDS = Seq(1, 2, 3).toDS()  // rdd操作变为dataSet操作
    primitiveDS.map(_ + 1).collect() // Returns: Array(2, 3, 4)

    // DataFrames can be converted to a Dataset by providing a class. Mapping will be done by name
    val path = "src/main/resources/people.json"
    val peopleDS = spark.read.json(path).as[Person]
    peopleDS.show()

  }
  // 和RDD 交互
  private def runInferSchemaExample(spark: SparkSession): Unit = {
    import spark.implicits._

    // Create an RDD of Person objects from a text file, convert it to a Dataframe
    val peopleDF = spark.sparkContext
      .textFile("src/main/resources/people.txt")
      .map(_.split(","))
      .map(attributes => Person(attributes(0), attributes(1).trim.toInt))
      .toDF()

    peopleDF.createOrReplaceTempView("people")
    val teenagersDF = spark.sql("SELECT name, age FROM people WHERE age BETWEEN 13 AND 19")
    teenagersDF.map(teenager => "Name: " + teenager(0)).show()
    // +------------+
    // |       value|
    // +------------+
    // |Name: Justin|
    // +------------+

    teenagersDF.map(teenager => "Name: " + teenager.getAs[String]("name")).show()
    // +------------+
    // |       value|
    // +------------+
    // |Name: Justin|
    // +------------+


  }

  private def runProgrammaticSchemaExample(spark: SparkSession): Unit = {

//    Create an RDD of Rows from the original RDD;
//    Create the schema represented by a StructType matching the structure of Rows in the RDD created in Step 1.
//    Apply the schema to the RDD of Rows via createDataFrame method provided by SparkSession.

    import spark.implicits._
    val peopleRDD = spark.sparkContext.textFile("src/main/resources/people.txt")
    val schemaString = "name age"
    // Generate the schema based on the string of schema
    val fields = schemaString.split(" ")
      .map(fieldName => StructField(fieldName, StringType, nullable = true))
    val schema = StructType(fields)

    // 将原来的RDD 转换为 Rows 用Row作为类型构造
    // Convert records of the RDD (people) to Rows
    val rowRDD = peopleRDD
      .map(_.split(","))
      .map(attributes => Row(attributes(0), attributes(1).trim))

    val peopleDF = spark.createDataFrame(rowRDD,schema)
    // Creates a temporary view using the DataFrame
    peopleDF.createOrReplaceTempView("people")

    // SQL can be run over a temporary view created using DataFrames
    val results = spark.sql("SELECT name FROM people")

    // The results of SQL queries are DataFrames and support all the normal RDD operations
    // The columns of a row in the result can be accessed by field index or by field name
    results.map(attributes => "Name: " + attributes(0)).show()

  }
  // 聚合  count(), countDistinct(), avg(), max(), min()


}
