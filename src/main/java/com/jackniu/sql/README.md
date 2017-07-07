## SQL and DataFrame
http://spark.apache.org/docs/latest/sql-programming-guide.html

A Dataset is a distributed collection of data. Dataset is a new interface added in Spark 1.6 that provides the benefits of RDDs (strong typing, ability to use powerful lambda functions) with the benefits of Spark SQL’s optimized execution engine.

SparkSession 构建:
    
    val spark = SparkSession.builder().getOrCreate()
    spark-shell: 

构建DataFrame，

    spark.read.json(".json") 属性名就是json的key
    自己生成: val caseClassDS = Seq(Person("Jack",12)).toDS() 
    spark.createDataFrame((1 to 100).map(i => Record(i, s"val_$i")))

操作: DataFrame， 

    查询 df.select("name").show()
    列: df.select($"name", $"age" + 1).show() 
        如果 df.select("name", "age" + 1).show()  则spark.sql.AnalysisException: cannot resolve '`age1`' given input columns: [age, name]; 
        因此，必须得加$ 这个符号用于转义吧（也没有更好的解释）

RDD 交互：
    
    .map(attributes => Person(attributes(0), attributes(1)).toDF()  将每一行数据都映射为Person类， 转换为DF
    查询: teenager.getAs[String]("name")
    
DataSource

    
Partition Discovery

    表格分块是一个很好的方法，在一个分块的表中，数据被存在不同的目录，分块列数据被编码的。
    parquet数据源可以自动发现分区信息。
    path
    └── to
        └── table
            ├── gender=male
            │   ├── ...
            │   │
            │   ├── country=US
            │   │   └── data.parquet
            │   ├── country=CN
            │   │   └── data.parquet
            │   └── ...
            └── gender=female
                ├── ...
                │
                ├── country=US
                │   └── data.parquet
                ├── country=CN
                │   └── data.parquet
                └── ...
    
    从Spark1.6.0 开始，分区仅仅在给定的路径下被发现，/path/to/table/gender=mail,gender 将不会被认为是一个分区列，
    
    
    val squaresDF = spark.sparkContext.makeRDD(1 to 5).map(i => (i, i * i)).toDF("value", "square")
        squaresDF.write.parquet("data/test_table/key=1")
    生成了一个DF，将他存储起来，不再是文件，而是一个分区表中
    
Merging Schema

    用户可以先建一个简单的schema，逐渐添加列来完善它。
    Since schema merging is a relatively expensive operation, and is not a necessity in most cases, we turned it off by default starting from 1.5.0. You may enable it by
    
        setting data source option mergeSchema to true when reading Parquet files (as shown in the examples below), or
        setting the global SQL option spark.sql.parquet.mergeSchema to true.


Hive metastore Parquet table conversion

    当向Hive metastore中读写Parquet表时，Spark SQL将使用Spark SQL自带的Parquet SerDe（SerDe：Serialize/Deserilize的简称,
    目的是用于序列化和反序列化），而不是用Hive的SerDe，Spark SQL自带的SerDe拥有更好的性能。这个优化的配置参数为
    spark.sql.hive.convertMetastoreParquet，默认值为开启。
    
    从表Schema处理的角度对比Hive和Parquet，有两个区别：
    
        Hive区分大小写，Parquet不区分大小写
        hive允许所有的列为空，而Parquet不允许所有的列全为空

      由于这两个区别，当将Hive metastore Parquet表转换为Spark SQL Parquet表时，需要将Hive metastore schema和Parquet schema进行一致化。
      一致化规则如下：
        
        
    这两个schema中的同名字段必须具有相同的数据类型。一致化后的字段必须为Parquet的字段类型。这个规则同时也解决了空值的问题。
    一致化后的schema只包含Hive metastore中出现的字段。
        忽略只出现在Parquet schema中的字段
        只在Hive metastore schema中出现的字段设为nullable字段，并加到一致化后的schema中

    元数据刷新（Metadata Refreshing）
    
    Spark SQL缓存了Parquet元数据以达到良好的性能。当Hive metastore Parquet表转换为enabled时，表修改后缓存的元数据并不能刷新。所以，当表被Hive或其它工具修改时，则必须手动刷新元数据，以保证元数据的一致性。示例如下：
    
        Scala
    
    // sqlContext is an existing HiveContext
    sqlContext.refreshTable("my_table")
    
    可以使用SQLContext的setConf方法或使用SQL执行 set  key=value  配置parquet
    

访问不同版本的Hive Metastore（Interacting with Different Versions of Hive Metastore）

    Spark SQL经常需要访问Hive metastore，Spark SQL可以通过Hive metastore获取Hive表的元数据。从Spark 1.4.0开始，Spark SQL只需简单的配置，
    就支持各版本Hive metastore的访问。注意，涉及到metastore时Spar SQL忽略了Hive的版本。Spark SQL内部将Hive反编译至Hive 1.2.1版本，
    Spark SQL的内部操作(serdes, UDFs, UDAFs, etc)都调用Hive 1.2.1版本的class。
