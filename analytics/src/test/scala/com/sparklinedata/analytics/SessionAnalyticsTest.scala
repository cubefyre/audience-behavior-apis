package com.sparklinedata.analytics

import com.spaklinedata.analytics.SessionAnalytics
import com.sparklinedata.analytics.base.TestHive
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql._
import org.scalatest.{FunSuite, BeforeAndAfterAll, GivenWhenThen}

import org.joda.time.format.DateTimeFormatter
import org.joda.time.{Seconds, DateTime}
import com.github.nscala_time.time.Imports._

import org.apache.spark.sql.test.TestSQLContext

import org.sparklinedata.spark.dateTime.Functions

class SessionAnalyticsTest extends FunSuite with BeforeAndAfterAll with GivenWhenThen{

  val sqlContext = new SQLContext(TestSQLContext.sparkContext)

  override def beforeAll() = {
    Functions.register(sqlContext)
  }

  test("testS3") {
    /*
    def weekendOnlyExpr : Expression = ((UserAnalytics.dateExpr dayOfWeekName) === "Saturday") ||
      ((UserAnalytics.dateExpr dayOfWeekName) === "Sunday")

        val sqlSt = date"select count(1) from sd_session_metrics where $weekendOnlyExpr"

        sql(sqlSt).show()
     */
    val jsonDF = sqlContext.read.json("s3n://tria.logs.segment/segment-logs/gAbtD2Qaw9/*/*.gz")
    jsonDF.take(1).foreach { r =>
      r.schema.printTreeString()
      println(r)
      r
    }
  }

  test("LoadSessionCube") {
    SessionAnalytics.loadCube(sqlContext)
    SessionAnalytics.trend(sqlContext)
  }

  test("WeekendSessionsOnly") {
    SessionAnalytics.loadCube(sqlContext)
    SessionAnalytics.trend(sqlContext, weekendOnly = true)
  }
}