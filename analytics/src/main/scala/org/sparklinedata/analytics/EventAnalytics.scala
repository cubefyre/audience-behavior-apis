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

import org.apache.spark.sql.{Column, SQLContext, DataFrame}
import org.apache.spark.sql.catalyst.analysis.{UnresolvedAttribute, UnresolvedExtractValue, UnresolvedFunction}
import org.apache.spark.sql.catalyst.plans.logical.{RepartitionByExpression, SortPartitions, Generate, LogicalPlan}

/**
 * Created by Jitender on 8/7/15.
 */
object Events extends BaseAnalytics with TrendAnalysis with SegmentAnalysis{

  val eventColName: String = "event_name"
  val pageColName: String = "page_name"
  var eventNames : Seq[String] = Seq()
  val eventAggCol = AggregateOnCol(eventColName, "Events", "countNotNull")
  val pageAggCol = AggregateOnCol(pageColName, "PageViews", "count")

  def defaultAggrCols: Seq[AggregateOnCol] = {
    Seq(pageAggCol, eventAggCol)
  }

  /*
  * Load cube for event analysis
  */
  def loadCube(sqlContext : SQLContext, lookbackDays: Int = 60) : Unit = {
    beforeAll(sqlContext)
    val df = loadCubeInMemory(sqlContext, "sd_events")
    eventsDF = Some(df.get.withColumn(weekAttrName, new Column(woyExpr)).cache())
    println(s"\tCube has ${eventsDF.get.count()} records.")
  }

  /*
   * trend analysis
  */
  def trend(
            sqlContext : SQLContext,
            analysisPeriod : Int = 7,
            eventList : Seq[String] = Seq(),
            includePageViewsTrend : Boolean = true,
            includeEventsTrend : Boolean = true,
            periodType : String = "daily",
            movingAverage : Option[Int] = None,
            segmentBy: Option[String] = None,
            topNSegments : Int = 10,
            filterOn: Option[String] = None,
            filterOp: String = "eq",
            filterVal: Option[Any] = None,
            weekendOnly : Boolean = false,
            verbose : Boolean = false) : Unit = {

    var aggregateOnCols = defaultAggrCols ++ eventList.map(
      eventName => AggregateOnCol(eventColName, eventName, "countIf", eventName))

    if(!includePageViewsTrend) aggregateOnCols = aggregateOnCols.filterNot(_.name == pageColName)
    if(!includeEventsTrend) aggregateOnCols = aggregateOnCols.filterNot(_.name == eventColName)

    val (resultDF,outputColNames) = trendG(eventsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)
    printTrendOutput(resultDF, outputColNames)
  }

  /*
   * trend single
  */
  def trendAny(
                sqlContext : SQLContext,
                analysisPeriod : Int = 7,
                aggregateOnCol: AggregateOnCol = eventAggCol,
                periodType : String = "daily",
                movingAverage : Option[Int] = None,
                segmentBy: Option[String] = None,
                topNSegments : Int = 10,
                filterOn: Option[String] = None,
                filterOp: String = "eq",
                filterVal: Option[Any] = None,
                weekendOnly : Boolean = false,
                verbose : Boolean = false
                ) : Unit = {

    val aggregateOnCols = Seq(aggregateOnCol)
    val (resultDF,outputColNames) = trendG(eventsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)
    printTrendOutput(resultDF, outputColNames)
  }

  /*
  * segment multiple cols over a period
  */
  def segment(
                sqlContext: SQLContext,
                analysisPeriod: Int = 7,
                eventList : Seq[String] = Seq(),
                includeDefaults : Boolean = true,
                segmentBy: Option[String] = None,
                topNSegments : Int = 10,
                filterOn: Option[String] = None,
                filterOp: String = "eq",
                filterVal: Option[Any] = None,
                sortBy : Seq[String] = Seq(),
                verbose: Boolean = false
               ) : Unit = {

    var aggregateOnCols = eventList.map(
      eventName => AggregateOnCol(eventColName, eventName, "countIf", eventName))
    if(includeDefaults)
      aggregateOnCols = defaultAggrCols ++ aggregateOnCols
    val (resultDF,outputColNames) = segmentG(eventsDF.get, sqlContext, aggregateOnCols, analysisPeriod,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, sortBy, verbose)

    printSegmentOutput(resultDF, outputColNames)
  }

  /*
  * segment single col over a period
  */
  def segmentAny(
                  sqlContext: SQLContext,
                  aggregateOnCol: AggregateOnCol = eventAggCol,
                  analysisPeriod: Int = 7,
                  segmentBy: Option[String] = None,
                  topNSegments : Int = 10,
                  filterOn: Option[String] = None,
                  filterOp: String = "eq",
                  filterVal: Option[Any] = None,
                  sortBy : Seq[String] = Seq(),
                  verbose: Boolean = false
                  ) : Unit = {

    val aggregateOnCols = Seq(aggregateOnCol)
    val (resultDF,outputColNames) = segmentG(eventsDF.get, sqlContext, aggregateOnCols, analysisPeriod,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, sortBy, verbose)

    printSegmentOutput(resultDF, outputColNames)

  }

}

