package com.sparklinedata.analytics.base

import org.apache.spark.sql.hive.test.TestHiveContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.test.TestSQLContext


object TestSparkContext {

  def newContext = new SparkContext("local[2]", "TestSQLContext",
    new SparkConf().set("spark.sql.test", "")
    // set("spark.jars", "/Users/hbutani/.m2/repository.sav/org/sparkline/hive/udfs/0.0.1-SNAPSHOT/udfs-0.0.1-SNAPSHOT.jar")
    /*.
        - prefix hadoop options with spark.hadoop
        - for a while thought SPARK-8093 issue was related to parallelism
      set("spark.hadoop.parquet.metadata.read.parallelism", "1")*/)
}

object TestHive extends TestHiveContext(TestSparkContext.newContext)

object TestSparkline extends TestHiveContext(TestSparkContext.newContext) {

  def runHive(cmd: String, maxRows: Int = 1000): Seq[String] =
    super.runSqlHive(cmd)
}