## Spark Streaming

http://spark.apache.org/docs/latest/streaming-programming-guide.html

SparkStreaming 的运行上下文就是StreamingContext 

    new StreamingContext(sparkConf(),Seconds(1))
    spark-shell: 中存在
    
    scala> sc
    res0: org.apache.spark.SparkContext = org.apache.spark.SparkContext@2f3cf7b9
    

### 运行分析
a)  Spark Streaming中不断的有数据流进来，他会把数据积攒起来，积攒的依据是以Batch Interval的方式进行积攒的，例如1秒钟，但是这1秒钟里面会有很多的数据例如event，
event就构成了一个数据的集合，而RDD处理的时候，是基于固定不变的集合产生RDD。实际上10秒钟产生一个作业的话，
就基于这10个event进行处理，对于连续不断的流进来的数据，就会根据这个连续不断event构成batch，因为时间间隔是固定的，
所以每个时间间隔产生的数据也是固定的，基于这些batch就会生成RDD的依赖关系。

### 集成数据源
For ingesting data from sources like Kafka, Flume, and Kinesis that are not present in the Spark Streaming core API, you will have to add the corresponding artifact spark-streaming-xyz_2.11 to the dependencies. For example, some of the common ones are as follows.
Source	Artifact

    Kafka 	spark-streaming-kafka-0-8_2.11
    Flume 	spark-streaming-flume_2.11
    Kinesis  spark-streaming-kinesis-asl_2.11 [Amazon Software License] 
    
 Points to remember:
 
     Once a context has been started, no new streaming computations can be set up or added to it.
     Once a context has been stopped, it cannot be restarted.
     Only one StreamingContext can be active in a JVM at the same time.
     stop() on StreamingContext also stops the SparkContext. To stop only the StreamingContext, set the optional parameter of stop() called stopSparkContext to false.
     A SparkContext can be re-used to create multiple StreamingContexts, as long as the previous StreamingContext is stopped (without stopping the SparkContext) before the next StreamingContext is created.


### Discretized Streams (DStreams)
内部，一个DStream被一连串的RDD代替，时间划分，每一个时间内的数据被认为是一个RDD
任何作用于DStream的算子，其实都会被转化为对其内部RDD的操作。例如，在前面的例子中，
我们将 lines 这个DStream转成words DStream对象，其实作用于lines上的flatMap算子，
会施加于lines中的每个RDD上，并生成新的对应的RDD，而这些新生成的RDD 对象就组成了words这个DStream对象。其过程如下图所示：


输入DStream和接收器

Spark Streaming主要提供两种内建的流式数据源：

    基础数据源（Basic sources）: 在StreamingContext API 中可直接使用的源，如：文件系统，套接字连接或者Akka actor。
    高级数据源（Advanced sources）: 需要依赖额外工具类的源，如：Kafka、Flume、Kinesis、Twitter等数据源。这些数据源都需要增加额外的依赖，详见依赖链接（linking）这一节。

注意。如果你需要同时才能够多个数据源拉取数据，就需要创建多个DStream对象。多个DStream对象其实也就同时创建了多个数据流接收器。
但是Spark的worker/executor都是长期运行的，因此它们都会各自占用一个分配给Spark Streaming的CPU(几个流就占几个CPU)，因此，在运行Spark Streaming
的时候，需要注意分配足够的CPU core来处理接收到的数据，同时还要有足够的CPU core来运行这些接收器。


* 如果本地运行Spark Streaming应用，不能将master设置为local或local[1]，这两个值都会只在本地启动一个线程。 此时如果使用一个包含接收器(套接字，Kafka，Flume)
的输入DStream，那么这一个线程只能用于运行这个接收器，而处理数据的逻辑就没有线程来执行了。一定要将master设置为local[n]， n> 接收器个数

http://spark.apache.org/docs/latest/configuration.html#spark-properties

* 将Spark Streaming应用置于集群中运行时，同样，分配给该应用的CPU core必须大于接收器的总数，否则，该应用就只会接受数据，不会处理数据。

### Basic Sources
基础数据源

* ssc.fileStream[KeyClass,ValueClass,InputFormatClass](dataDirectory):将会监视在dataDirectory中创建的任何文件
    文件必须有相同的格式
    文件必须在dataDirectory中创建
    一旦文件move进dataDirectory之后，就不能再改动。所以如果这个文件后续还有写入，这些新写入的数据不会被读取。

* 基于Custom Receivers的Streams

高级数据源



    Kafka: Spark Streaming 2.1.1 is compatible with Kafka broker versions 0.8.2.1 or higher. See the Kafka Integration Guide for more details.

    Flume: Spark Streaming 2.1.1 is compatible with Flume 1.6.0. See the Flume Integration Guide for more details.

    Kinesis: Spark Streaming 2.1.1 is compatible with Kinesis Client Library 1.2.1. See the Kinesis Integration Guide for more details.

Transformations on DStreams

    Transformation	Meaning
    map(func) 	Return a new DStream by passing each element of the source DStream through a function func.
    flatMap(func) 	Similar to map, but each input item can be mapped to 0 or more output items.
    filter(func) 	Return a new DStream by selecting only the records of the source DStream on which func returns true.
    repartition(numPartitions) 	Changes the level of parallelism in this DStream by creating more or fewer partitions.
    union(otherStream) 	Return a new DStream that contains the union of the elements in the source DStream and otherDStream.
    count() 	Return a new DStream of single-element RDDs by counting the number of elements in each RDD of the source DStream.
    reduce(func) 	Return a new DStream of single-element RDDs by aggregating the elements in each RDD of the source DStream using a function func (which takes two arguments and returns one). The function should be associative and commutative so that it can be computed in parallel.
    countByValue() 	When called on a DStream of elements of type K, return a new DStream of (K, Long) pairs where the value of each key is its frequency in each RDD of the source DStream.
    reduceByKey(func, [numTasks]) 	When called on a DStream of (K, V) pairs, return a new DStream of (K, V) pairs where the values for each key are aggregated using the given reduce function. Note: By default, this uses Spark's default number of parallel tasks (2 for local mode, and in cluster mode the number is determined by the config property spark.default.parallelism) to do the grouping. You can pass an optional numTasks argument to set a different number of tasks.
    join(otherStream, [numTasks]) 	When called on two DStreams of (K, V) and (K, W) pairs, return a new DStream of (K, (V, W)) pairs with all pairs of elements for each key.
    cogroup(otherStream, [numTasks]) 	When called on a DStream of (K, V) and (K, W) pairs, return a new DStream of (K, Seq[V], Seq[W]) tuples.
    transform(func) 	Return a new DStream by applying a RDD-to-RDD function to every RDD of the source DStream. This can be used to do arbitrary RDD operations on the DStream.
    updateStateByKey(func) 	Return a new "state" DStream where the state for each key is updated by applying the given function on the previous state of the key and the new values for the key. This can be used to maintain arbitrary state data for each key.

























