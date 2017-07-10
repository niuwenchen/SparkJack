package com.jackniu.streaming

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Created by JackNiu on 2017/7/10.
  */
object NetworkWordCount {
  def main(args: Array[String]): Unit = {
    val sparkConf = new SparkConf().setAppName("NetworkWordCount")
    val ssc = new StreamingContext(sparkConf, Seconds(1))
    val lines = ssc.socketTextStream("localhost",9999)// 创建一个DStream
//    val lines = ssc.socketStream()
    val words = lines.flatMap(_.split(" ")) //one-to-many DStream 操作
    val pairs = words.map(word => (word, 1))
    val wordCounts =pairs.reduceByKey(_+_)
    wordCounts.print()

    ssc.start()
    ssc.awaitTermination() // wait for the computation to terminate

    // nc -lk 9999

  }
}
