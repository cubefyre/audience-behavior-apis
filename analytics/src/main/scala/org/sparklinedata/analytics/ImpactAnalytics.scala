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

/**
 * Created by Jitender on 8/24/15.
 */

import org.sparklinedata.analytics.Impact._
import org.sparklinedata.analytics.utils.DataframeUtils._
import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute
import org.apache.spark.sql.catalyst.plans.logical.RepartitionByExpression
import org.apache.spark.sql.{Column, SQLContext}
import org.sparklinedata.spark.dateTime.dsl.expressions._

object Impact extends BaseAnalytics with TrendAnalysis with SegmentAnalysis {
  val impactEventTimeAttrName : String = "sd_goal_date_time_utc"
  val impactEventColName : String = "sd_impact_event_name"
  val impactPageColName : String = "sd_impact_page_path"
  val productQuantityColName : String = "sd_product_quantity"
  val productRevColName : String = "sd_product_revenue"

  val daysToGoalColName: String = "sd_days_to_goal" // days to goal
  val sessionsToGoalColName: String = "sd_sessions_to_goal" // sessions to goal
  val timeToGoalColName: String = "sd_time_to_goal" // time to goal
  val userColName : String = "sd_goal_user_id" // user_id
  val goalColName : String = "sd_goal_session_id" // session id
  val mtRevAttr : String = "sd_multi_touch_attr_revenue" // multi-touch revenue attributed
  val impactEventRank : String = "impact_event_group_rank"

  val goalEventTimeAttrName : String = "sd_goal_date_time_utc"
  val goalEventDateExpr = dateTime(UnresolvedAttribute(goalEventTimeAttrName))

  val daysToGoalAggCol = AggregateOnCol(daysToGoalColName, "AvgDaysToGoal", "avg")
  val sessionsToGoalAggCol = AggregateOnCol(sessionsToGoalColName, "AvgSessionsToGoal", "avg")
  val timeToGoalAggCol = AggregateOnCol(timeToGoalColName, "AvgTimeToGoal", "avg")
  val goalsCountAggCol = AggregateOnCol(goalColName, "CountOfGoals", "countd")
  val userCountAggCol = AggregateOnCol(userColName, "CountOfUsers", "countd")
  val revAggCol = AggregateOnCol(productRevColName, "Revenues", "sum")
  val mtRevAttrAggCol = AggregateOnCol(mtRevAttr, "AttributedRevenues", "sum")

  def defaultAggrCols: Seq[AggregateOnCol] = {
    Seq()
  }
  /*
  * Load cube for impact analysis
  */
  def loadCube(sqlContext : SQLContext, lookbackDays: Int = 60) : Unit = {
    beforeAll(sqlContext)
    goalsDF = Some(loadCubeInMemory(sqlContext, "sd_goals").get.cache())
    //goalsDF = Some(df.get.withColumn(weekAttrName, new Column(woyExpr)).cache())
    println(s"\tCube has ${goalsDF.get.count()} records.")
  }


  /*
  * impact events or pages over a period
  */
  def impact(
               sqlContext: SQLContext,
               analysisPeriod: Int = 7,
               includeDaysToGoal : Boolean = false,
               includeSessionsToGoal : Boolean = false,
               includeTimeToGoal : Boolean = false,
               groupBy : Option[String] = None,
               topN : Int = 5,
               filterOn: Option[String] = None,
               filterOp: String = "eq",
               filterVal: Option[Any] = None,
               sortBy : Seq[String] = Seq(),
               aggFilterOn : Option[String] = None,
               aggFilterOp: String = "eq",
               aggFilterVal: Option[Any] = None,
               verbose: Boolean = false
               ) : Unit = {

    val inAnalysisPeriodExpr = getPeriodExpr(goalEventDateExpr, currentDate, analysisPeriod, "day")
    var aggregateOnCols = defaultAggrCols
    if(includeDaysToGoal) aggregateOnCols = aggregateOnCols :+ daysToGoalAggCol
    if(includeSessionsToGoal) aggregateOnCols = aggregateOnCols :+ sessionsToGoalAggCol
    if(includeTimeToGoal) aggregateOnCols = aggregateOnCols :+ timeToGoalAggCol

    val (resultDF, outputColNames) = segmentOverPeriod(goalsDF.get, sqlContext, aggregateOnCols, inAnalysisPeriodExpr,
      groupBy, topN, filterOn, filterOp, filterVal, sortBy, verbose)

    val finalDF = aggFilterOn match {
      case None => resultDF
      case rankDim => filterDF(resultDF, getFilterExpr(aggFilterOn.get, aggFilterOp, aggFilterVal.getOrElse(10)))
    }
    val firstColTitle = groupBy.get match {
      case `impactEventColName` => "ImpactEvent"
      case `impactPageColName` => "ImpactPage"
    }
    printSegmentOutput(finalDF, outputColNames.updated(0,groupBy.get))
  }

  def topEvents(
               sqlContext: SQLContext,
               analysisPeriod: Int = 7,
               includeDaysToGoal : Boolean = false,
               includeSessionsToGoal : Boolean = false,
               includeTimeToGoal : Boolean = false,
               topN : Int = 5,
               filterOn: Option[String] = None,
               filterOp: String = "eq",
               filterVal: Option[Any] = None,
               sortBy : Seq[String] = Seq(),
               aggFilterOn : Option[String] = None,
               aggFilterOp: String = "eq",
               aggFilterVal: Option[Any] = None,
               verbose: Boolean = false
               ) : Unit = {

    val groupBy: Option[String] = Some(impactEventColName)
    impact(sqlContext, analysisPeriod, includeDaysToGoal, includeSessionsToGoal, includeTimeToGoal,
      groupBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, verbose)
  }


  def topPages(
               sqlContext: SQLContext,
               analysisPeriod: Int = 7,
               includeDaysToGoal : Boolean = false,
               includeSessionsToGoal : Boolean = false,
               includeTimeToGoal : Boolean = false,
               topN : Int = 5,
               filterOn: Option[String] = None,
               filterOp: String = "eq",
               filterVal: Option[Any] = None,
               sortBy : Seq[String] = Seq(),
               aggFilterOn : Option[String] = None,
               aggFilterOp: String = "eq",
               aggFilterVal: Option[Any] = None,
               verbose: Boolean = false
               ) : Unit = {

    val groupBy: Option[String] = Some(impactPageColName)
    impact(sqlContext, analysisPeriod, includeDaysToGoal, includeSessionsToGoal, includeTimeToGoal,
      groupBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, verbose)
  }

  /*
  impact of events or pages on users, revenues
   */
  def impactOn(
                 sqlContext: SQLContext,
                 analysisPeriod: Int = 7,
                 groupBy : Option[String] = None,
                 topN : Int = 5,
                 filterOn: Option[String] = None,
                 filterOp: String = "eq",
                 filterVal: Option[Any] = None,
                 sortBy : Seq[String] = Seq(),
                 aggFilterOn : Option[String] = None,
                 aggFilterOp: String = "eq",
                 aggFilterVal: Option[Any] = None,
                 dim : String = "users",
                 verbose: Boolean = false
                 ) : Unit = {

    val aggregateOnCols = dim match {
      case "users" => defaultAggrCols :+ userCountAggCol
      case "revenues" => defaultAggrCols :+ revAggCol
      case "mtAttRev" => defaultAggrCols :+ mtRevAttrAggCol
    }
    val inAnalysisPeriodExpr = getPeriodExpr(goalEventDateExpr, currentDate, analysisPeriod, "day")

    val (resultDF, outputColNames) = segmentOverPeriod(goalsDF.get, sqlContext, aggregateOnCols, inAnalysisPeriodExpr,
      groupBy, topN, filterOn, filterOp, filterVal, sortBy, verbose)

    val finalDF = aggFilterOn match {
      case None => resultDF
      case rankDim => filterDF(resultDF, getFilterExpr(aggFilterOn.get, aggFilterOp, aggFilterVal.getOrElse(10)))
    }
    printSegmentOutput(finalDF, outputColNames.updated(0,groupBy.get))
  }

  def topEventsByUsers(
                        sqlContext: SQLContext,
                        analysisPeriod: Int = 7,
                        topN : Int = 5,
                        filterOn: Option[String] = None,
                        filterOp: String = "eq",
                        filterVal: Option[Any] = None,
                        sortBy : Seq[String] = Seq(),
                        aggFilterOn : Option[String] = None,
                        aggFilterOp: String = "eq",
                        aggFilterVal: Option[Any] = None,
                        verbose: Boolean = false
                        ) : Unit = {

    val segmentBy: Option[String] = Some(impactEventColName)
    impactOn(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal)
  }

  def topPagesByUsers(
                        sqlContext: SQLContext,
                        analysisPeriod: Int = 7,
                        topN : Int = 5,
                        filterOn: Option[String] = None,
                        filterOp: String = "eq",
                        filterVal: Option[Any] = None,
                        sortBy : Seq[String] = Seq(),
                        aggFilterOn : Option[String] = None,
                        aggFilterOp: String = "eq",
                        aggFilterVal: Option[Any] = None,
                        verbose: Boolean = false
                        ) : Unit = {

    val segmentBy: Option[String] = Some(impactPageColName)
    impactOn(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal)
  }


  def topEventsByRevenues(
                        sqlContext: SQLContext,
                        analysisPeriod: Int = 7,
                        topN : Int = 5,
                        filterOn: Option[String] = None,
                        filterOp: String = "eq",
                        filterVal: Option[Any] = None,
                        sortBy : Seq[String] = Seq(),
                        aggFilterOn : Option[String] = None,
                        aggFilterOp: String = "eq",
                        aggFilterVal: Option[Any] = None,
                        verbose: Boolean = false
                        ) : Unit = {

    val segmentBy: Option[String] = Some(impactEventColName)
    impactOn(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, "revenues", verbose)
  }

  def topPagesByRevenues(
                       sqlContext: SQLContext,
                       analysisPeriod: Int = 7,
                       topN : Int = 5,
                       filterOn: Option[String] = None,
                       filterOp: String = "eq",
                       filterVal: Option[Any] = None,
                       sortBy : Seq[String] = Seq(),
                       aggFilterOn : Option[String] = None,
                       aggFilterOp: String = "eq",
                       aggFilterVal: Option[Any] = None,
                       verbose: Boolean = false
                       ) : Unit = {
    val segmentBy: Option[String] = Some(impactPageColName)
    impactOn(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, "revenues", verbose)
  }


  def topEventsByMultiTocuhAttribution(
                          sqlContext: SQLContext,
                          analysisPeriod: Int = 7,
                          topN : Int = 5,
                          filterOn: Option[String] = None,
                          filterOp: String = "eq",
                          filterVal: Option[Any] = None,
                          sortBy : Seq[String] = Seq(),
                          aggFilterOn : Option[String] = None,
                          aggFilterOp: String = "eq",
                          aggFilterVal: Option[Any] = None,
                          verbose: Boolean = false
                          ) : Unit = {
    val segmentBy: Option[String] = Some(impactEventColName)
    impactOn(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, "mtAttRev", verbose)
  }

  def topPagesByMultiTocuhAttribution(
                                       sqlContext: SQLContext,
                                       analysisPeriod: Int = 7,
                                       topN : Int = 5,
                                       filterOn: Option[String] = None,
                                       filterOp: String = "eq",
                                       filterVal: Option[Any] = None,
                                       sortBy : Seq[String] = Seq(),
                                       aggFilterOn : Option[String] = None,
                                       aggFilterOp: String = "eq",
                                       aggFilterVal: Option[Any] = None,
                                       verbose: Boolean = false
                                       ) : Unit = {
    val segmentBy: Option[String] = Some(impactPageColName)
    impactOn(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, "mtAttRev", verbose)
  }

  /*
   * Last-touch attribution - common method for events and pages
   */
  def topLastTouchAttribution (
                                sqlContext: SQLContext,
                                analysisPeriod: Int = 7,
                                groupBy : Option[String] = None,
                                topN : Int = 5,
                                filterOn: Option[String] = None,
                                filterOp: String = "eq",
                                filterVal: Option[Any] = None,
                                sortBy : Seq[String] = Seq(),
                                aggFilterOn : Option[String] = None,
                                aggFilterOp: String = "eq",
                                aggFilterVal: Option[Any] = None,
                                verbose: Boolean = false
                              ) : Unit = {

    val analysisPeriodExpr = getPeriodExpr(goalEventDateExpr, currentDate, analysisPeriod, "day")

    val firstColTitle = groupBy.get match {
      case `impactEventColName` => "ImpactEvent"
      case `impactPageColName` => "ImpactPage"
    }

    //  A composite filter - filter on analysis period, filter on dimension as well as null columns
    val compositeFilterExpr = if (filterOn.isDefined && filterVal.isDefined) {
      val filterExpr = getFilterExpr(filterOn.get, filterOp, filterVal.get)
      Seq(new Column(analysisPeriodExpr), filterExpr, new Column(groupBy.get).isNotNull)
    } else {
      Seq(new Column(analysisPeriodExpr), new Column(groupBy.get).isNotNull)
    }

    val lastProdRevAggCol = AggregateOnCol(productRevColName, "sd_product_revenue", "last")
    val lastImpactEventAggCol = AggregateOnCol(groupBy.get, groupBy.get, "last")
    val aggregateOnColsPre = Seq(lastProdRevAggCol, lastImpactEventAggCol)

    //Impact.goalsDF.get.groupBy("sd_goal_session_id").agg(last("sd_product_revenue").as("sd_product_revenue"), last("sd_impact_event_name").as("sd_impact_event_name")).groupBy("sd_impact_event_name").agg(sum("sd_product_revenue")).take(50).foreach(println)
    val aggregateOnCols = Seq(AggregateOnCol(productRevColName, "AttributedRevenues", "sum"))
    val aggrColNames = aggregateOnCols.map(ac => ac.alias)
    val aggrMap = aggregateOnCols.map(ac => (ac.colExpr))
    val outputColNames = Seq(firstColTitle) ++ aggrColNames // for final output
    val orderByColNames = if (sortBy.length > 0) sortBy else aggrColNames
    val groupByColNames = Seq(groupBy.get)

    //filter for period, filter for null, groupby, aggr, orderby, limit step
    val resultDF =
      orderDF(
        aggDF( // sum all revenues by event
          groupByDF( // now group by event
            aggDF( // get last event in the list and revenues associated with the goal
                groupByDF(// groupby goal
                  orderDF( // order by impact dates
                      filterDF( // filter on time
                      goalsDF.get,
                      compositeFilterExpr: _*
                    ),
                    Seq(impactEventTimeAttrName)
                  ),
                Seq(goalColName)
              ), aggregateOnColsPre.map(ac => (ac.colExpr))
            ),
            groupByColNames
          ),
          aggrMap
        ),
        orderByColNames,
        "desc"
      ).limit(topN)

    val finalDF = aggFilterOn match {
      case None => resultDF
      case rankDim => filterDF(resultDF, getFilterExpr(aggFilterOn.get, aggFilterOp, aggFilterVal.getOrElse(10)))
    }
    printSegmentOutput(finalDF, outputColNames.updated(0,groupBy.get))
  }

  def topEventsByLastTocuhAttribution(
                                        sqlContext: SQLContext,
                                        analysisPeriod: Int = 7,
                                        topN : Int = 5,
                                        filterOn: Option[String] = None,
                                        filterOp: String = "eq",
                                        filterVal: Option[Any] = None,
                                        sortBy : Seq[String] = Seq(),
                                        aggFilterOn : Option[String] = None,
                                        aggFilterOp: String = "eq",
                                        aggFilterVal: Option[Any] = None,
                                        verbose: Boolean = false
                                        ) : Unit = {
    val segmentBy: Option[String] = Some(impactEventColName)
    topLastTouchAttribution(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, verbose)
  }

  def topPagesByLastTocuhAttribution(
                                       sqlContext: SQLContext,
                                       analysisPeriod: Int = 7,
                                       topN : Int = 5,
                                       filterOn: Option[String] = None,
                                       filterOp: String = "eq",
                                       filterVal: Option[Any] = None,
                                       sortBy : Seq[String] = Seq(),
                                       aggFilterOn : Option[String] = None,
                                       aggFilterOp: String = "eq",
                                       aggFilterVal: Option[Any] = None,
                                       verbose: Boolean = false
                                       ) : Unit = {
    val segmentBy: Option[String] = Some(impactPageColName)
    topLastTouchAttribution(sqlContext, analysisPeriod,
      segmentBy, topN, filterOn, filterOp, filterVal, sortBy, aggFilterOn, aggFilterOp, aggFilterVal, verbose)
  }

}
