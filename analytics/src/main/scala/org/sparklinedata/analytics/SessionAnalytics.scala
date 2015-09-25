/**
 * SparklineData, Inc. -- http://www.sparklinedata.com/
 *
 * Scala based Audience Behavior APIs
 *
 * Copyright 2014-2015 SparklineData, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sparklinedata.analytics

import org.apache.spark.sql.{Column, SQLContext}

/**
 * Created by Jitender on 8/7/15.
 */

object Sessions extends BaseAnalytics with TrendAnalysis with SegmentAnalysis {

  val sessionIdColName: String = "sd_session_id"
  val revSessionColName: String = "sd_is_revenue_session"
  val videoSessionColName: String = "sd_is_video_session"
  val cartSessionColName: String = "sd_is_cart_session"
  val bounceSessionColName: String = "sd_is_bounce_session"


  val sessionAggCol = AggregateOnCol(sessionIdColName, "Sessions", "count")
  val revSessionAggCol = AggregateOnCol(revSessionColName, "Revenue Sessions", "sum")
  val videoSessionAggCol = AggregateOnCol(videoSessionColName, "Video Sessions", "sum")
  val cartSessionAggCol = AggregateOnCol(cartSessionColName, "Cart Sessions", "sum")
  val bounceSessionAggCol = AggregateOnCol(bounceSessionColName, "Bounce Sessions", "sum")


  def defaultAggrCols: Seq[AggregateOnCol] = {
    Seq(sessionAggCol, revSessionAggCol)
  }

  def defaultBehaviorAggrCols: Seq[AggregateOnCol] = {
    Seq(sessionAggCol, revSessionAggCol, videoSessionAggCol, cartSessionAggCol)
  }

  // Load cube
  def loadCube(sqlContext: SQLContext, lookbackDays: Int = 60): Unit = {
    beforeAll(sqlContext)
    val woy = dateExpr weekOfWeekyear
    val df = loadCubeInMemory(sqlContext, "sd_session_metrics")
    sessionMetricsDF = Some(df.get.withColumn(weekAttrName, new Column(woy)).cache())
    println(s"\tCube has ${sessionMetricsDF.get.count()} records.")
  }

  /*
   * trend analysis
  */
  def trend(
              sqlContext : SQLContext,
              analysisPeriod : Int = 7,
              periodType : String = "daily",
              sessionsTrendOnly : Boolean = false,
              revenueSessionsTrendOnly : Boolean = false,
              movingAverage : Option[Int] = None,
              segmentBy: Option[String] = None,
              topNSegments : Int = 10,
              filterOn: Option[String] = None,
              filterOp: String = "eq",
              filterVal: Option[Any] = None,
              weekendOnly : Boolean = false,
              verbose : Boolean = false
             ) : Unit = {

    var aggregateOnCols = defaultAggrCols
    if(revenueSessionsTrendOnly) aggregateOnCols = aggregateOnCols.filter(_.name == revSessionColName)
    if(sessionsTrendOnly) aggregateOnCols = aggregateOnCols.filter(_.name == sessionIdColName)
    val (resultDF,outputColNames) = trendG(sessionMetricsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }

  /*
   * trend any
  */
  def trendAny(
                sqlContext : SQLContext,
                analysisPeriod : Int = 7,
                aggregateOnCol: AggregateOnCol = sessionAggCol,
                periodType : String = "daily",
                includeSessionTrend : Boolean = false,
                includeRevenueSessionTrend : Boolean = false,
                movingAverage : Option[Int] = None,
                segmentBy: Option[String] = None,
                topNSegments : Int = 10,
                filterOn: Option[String] = None,
                filterOp: String = "eq",
                filterVal: Option[Any] = None,
                weekendOnly : Boolean = false,
                verbose : Boolean = false
                ) : Unit = {

    var aggregateOnCols = Seq(aggregateOnCol)
    if(includeRevenueSessionTrend) aggregateOnCols = aggregateOnCols :+ revSessionAggCol
    if(includeSessionTrend) aggregateOnCols = aggregateOnCols :+ sessionAggCol

    val (resultDF,outputColNames) = trendG(sessionMetricsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }


  /*
  * segment values over a period
  */
  def segment(sqlContext: SQLContext,
              analysisPeriod: Int = 7,
              colList : Seq[String] = Seq(cartSessionColName, videoSessionColName),
              includeDefaults : Boolean = true,
              segmentBy: Option[String] = None,
              topNSegments : Int = 10,
              filterOn: Option[String] = None,
              filterOp: String = "eq",
              filterVal: Option[Any] = None,
              sortBy : Seq[String] = Seq(),
              verbose: Boolean = false
               ) : Unit = {

    var aggregateOnCols = colList.map(colName => AggregateOnCol(colName, colName, "sum"))

    if(includeDefaults) aggregateOnCols = defaultAggrCols ++ aggregateOnCols

    val (resultDF,outputColNames) = segmentG(sessionMetricsDF.get, sqlContext,aggregateOnCols, analysisPeriod,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, sortBy, verbose)

    printSegmentOutput(resultDF, outputColNames)
  }

  /*
  * segment any values over a period
  */
  def segmentAny(sqlContext: SQLContext,
                 analysisPeriod: Int = 7,
                 aggregateOnCol: AggregateOnCol = revSessionAggCol,
                 includeDefaults : Boolean = true,
                 segmentBy: Option[String] = None,
                 topNSegments : Int = 10,
                 filterOn: Option[String] = None,
                 filterOp: String = "eq",
                 filterVal: Option[Any] = None,
                 sortBy : Seq[String] = Seq(),
                 verbose: Boolean = false
                  ) : Unit = {

    val aggregateOnCols = Seq(aggregateOnCol)

    val (resultDF,outputColNames) = segmentG(sessionMetricsDF.get, sqlContext,aggregateOnCols, analysisPeriod,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, sortBy, verbose)

    printSegmentOutput(resultDF, outputColNames)
  }

  /*
   * behavior analysis
   * what did users do - video sessions, add-to-cart sessions, revenue sessions
   * exclusive behavior
  */
  def behavior(sqlContext : SQLContext,
               analysisPeriod : Int = 7,
               periodType : String = "daily",
               revenueSessionsOnly : Boolean = false,
               videoSessionOnly : Boolean = false,
               cartSessionsOnly : Boolean = false,
               bounceSessionsOnly : Boolean = false,
               movingAverage : Option[Int] = None,
               segmentBy: Option[String] = None,
               topNSegments : Int = 10,
               filterOn: Option[String] = None,
               filterOp: String = "eq",
               filterVal: Option[Any] = None,
               weekendOnly : Boolean = false,
               verbose : Boolean = false) : Unit = {

    var aggregateOnCols = defaultBehaviorAggrCols
    if(revenueSessionsOnly) aggregateOnCols = Seq(sessionAggCol) ++ defaultBehaviorAggrCols.filter(_.name == revSessionColName)
    if(videoSessionOnly) aggregateOnCols = Seq(sessionAggCol) ++ defaultBehaviorAggrCols.filter(_.name == videoSessionColName)
    if(cartSessionsOnly) aggregateOnCols = Seq(sessionAggCol) ++ defaultBehaviorAggrCols.filter(_.name == cartSessionColName)
    if(bounceSessionsOnly) aggregateOnCols = Seq(sessionAggCol) ++ defaultBehaviorAggrCols.filter(_.name == bounceSessionColName)

    val (resultDF,outputColNames) = trendG(sessionMetricsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)
    printTrendOutput(resultDF, outputColNames)
  }

  /*
   * inclusive behavior analysis - sessions in which users did collection of events
  */
  def behaviorInclusive(sqlContext : SQLContext,
                        analysisPeriod : Int = 7,
                        colList : Seq[String] = Seq(revSessionColName, cartSessionColName, videoSessionColName),
                        periodType : String = "daily",
                        movingAverage : Option[Int] = None,
                        segmentBy: Option[String] = None,
                        topNSegments : Int = 10,
                        filterOn: Option[String] = None,
                        filterOp: String = "eq",
                        filterVal: Option[Any] = None,
                        weekendOnly : Boolean = false,
                        verbose : Boolean = false) : Unit = {

    val filterCond = colList match { // all three, two or one
      case Seq(a, b, c) => new Column(a).eqNullSafe(1).and(new Column(b).eqNullSafe(1)).and(new Column(c).eqNullSafe(1))
      case Seq(a, b) => new Column(a).eqNullSafe(1).and(new Column(b).eqNullSafe(1))
      case Seq(a) => new Column(a).eqNullSafe(1)
      case _ => new Column(revSessionColName).eqNullSafe(1)
    }
    val sessionWithEvents = org.apache.spark.sql.functions.sum(
      org.apache.spark.sql.functions.when(filterCond === true, 1).otherwise(0))

    //val startingDF = sessionMetricsDF.get.filter(filterCond)
    val aggregateOnCols = Seq(sessionAggCol, AggregateOnColExpr("sessions_w_events", "SessionsWithEvents", sessionWithEvents))
      //defaultAggrCols.filter(_.name == sessionIdColName)

    val (resultDF,outputColNames) = trendG(sessionMetricsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }

}
