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
import org.apache.spark.sql.catalyst.expressions._
import org.joda.time.{Period, DateTime}
import org.sparklinedata.spark.dateTime.dsl.expressions._

import org.sparklinedata.analytics.utils.DataframeUtils._

/**
 * Created by Jitender on 8/7/15.
 */
object Users  extends BaseAnalytics with TrendAnalysis  with SegmentAnalysis {

  val userColName: String = "user_id"
  val newUserColName: String = "sd_is_new_user"
  val revUserColName : String = "sd_is_revenue_user"
  val rankByTimeColName : String = "sd_rank_by_time_spent"
  val rankBySessionCountColName : String = "sd_rank_by_session_count"
  val rankByEventCountColName : String = "sd_rank_by_event_count"
  val firstSeentAtColName = "sd_first_seen_at"

  val userAggCol = AggregateOnCol(userColName, "Users", "countd")
  val allUserAggCol = AggregateOnCol(userColName, "Users", "count")
  val newUserAggCol = AggregateOnCol(newUserColName, "New Users", "sum")
  val retUserAggCol = AggregateOnCol(newUserColName, "Returning Users", "countIf", 0)
  val revUserAggCol = AggregateOnCol(revUserColName, "Revenue Users", "sum")
  val convRateAggColExpr = revUserAggCol.colExprWOAlias.divide(allUserAggCol.colExprWOAlias).multiply(100)
  val convRateAggCol = AggregateOnColExpr("sd_conv_rate", "ConversionRate", convRateAggColExpr)

  def defaultAggrCols: Seq[AggregateOnCol] = Seq(userAggCol)

  // engagement metrics
  val userSessionCountColName = "sd_session_count"
  val userTimeSpentColName = "sd_time_spent"
  val userEventCountColName = "sd_event_count"

  val userSessionCountAggCol = AggregateOnCol(userSessionCountColName, "Avg Count of Sessions", "avg")
  //val userTimeSpentAggCol = AggregateOnCol(userTimeSpentColName, "Avg Time Spent", "timeInSecs")
  val userTimeSpentAggCol = AggregateOnCol(userTimeSpentColName, "Avg Time Spent", "avg")
  val userEventCountAggCol = AggregateOnCol(userEventCountColName, "Avg Count of Events", "avg")

  def defaultEngagementAggrCols: Seq[AggregateOnCol] = Seq(userSessionCountAggCol,
    userEventCountAggCol, userTimeSpentAggCol)

  // behavioral metrics
  val expr = "select concat(sd_year, '-', sd_month, '-', sd_day) as date, sum(sd_num_revenue_session) as rev_users, sum(sd_num_cart_session) as cart_users, sum(sd_num_video_session) as video_users from sd_user_metricsII"
  val revSessionsColName = "sd_num_revenue_session"
  val cartSessionsColName = "sd_num_cart_session"
  val videoSessionsColName = "sd_num_video_session"

  val revSessionsAggCol = AggregateOnCol(revSessionsColName, "Revenue Sessions", "sum")
  val cartSessionsAggCol = AggregateOnCol(cartSessionsColName, "Cart Sessions", "sum")
  val videoSessionsAggCol = AggregateOnCol(videoSessionsColName, "Video Sessions", "sum")

  def defaultBehaviorAggrCols: Seq[AggregateOnCol] = Seq(revSessionsAggCol,
    cartSessionsAggCol, videoSessionsAggCol)


  /*
  * load cube for user metrics analysis
  */
  def loadCube(sqlContext : SQLContext, lookbackDays: Int = 60) : Unit = {
    beforeAll(sqlContext)

    val df = loadCubeInMemory(sqlContext, "sd_user_metrics")
    //df.get.registerTempTable("sd_user_metrics")

    val inLookbackPeriodExpr = getPeriodExpr(dateExpr,currentDate, lookbackDays)
    /*
    This step should go into ETL Stage II - tagging users as new or returning
    Tag users as new users if they have never been seen before
    Get user ids and when first seen at
    */
    val lastNDaysUsers = sqlContext.sql(date"select user_id, min($dateInMillisExpr) as sd_first_seen_at from sd_user_metrics where $inLookbackPeriodExpr group by user_id")
    lastNDaysUsers.registerTempTable("users_last_n_days")

    /* Now tag if first seen is less than the daily(session) date
    also fill nulls with other
    */
    val query =
      date"""
            | select a.*, b.sd_first_seen_at,
            | $woyExpr as sd_week,
            | CASE WHEN b.sd_first_seen_at < $dateInMillisExpr THEN 0 ELSE 1 END AS sd_is_new_user,
            | CASE WHEN a.sd_daily_revenue > 0 THEN 1 ELSE 0 END AS sd_is_revenue_user
            | from sd_user_metrics a, users_last_n_days b where a.user_id=b.user_id
          """.stripMargin
    userMetricsDF = Some(sqlContext.sql(query).na.fill("other").cache())
    println(s"\tCube has ${userMetricsDF.get.count()} records.")
    userMetricsDF.get.registerTempTable("sd_user_metricsII")
  }

  /*
   * trends
  */
  def trend(sqlContext : SQLContext,
            analysisPeriod : Int = 7,
            periodType : String = "daily",
            includeUsersTrend : Boolean = true,
            includeNewUsersTrend : Boolean = false,
            includeReturningUsersTrend : Boolean = false,
            includeRevenueUsersTrend : Boolean = false,
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
    if(includeNewUsersTrend) aggregateOnCols = aggregateOnCols :+ newUserAggCol
    if(includeReturningUsersTrend) aggregateOnCols = aggregateOnCols :+ retUserAggCol
    if(includeRevenueUsersTrend) aggregateOnCols = aggregateOnCols :+ revUserAggCol
    if(!includeUsersTrend) aggregateOnCols = aggregateOnCols.filterNot(_.name == userColName)

    val (resultDF,outputColNames) = trendG(userMetricsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }

  /*
   * trend any
  */
  def trendAny(sqlContext : SQLContext,
               analysisPeriod : Int = 7,
               aggregateOnCol: AggregateOnCol = userAggCol,
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

    var aggregateOnCols = Seq(userAggCol, aggregateOnCol)
    val (resultDF,outputColNames) = trendG(userMetricsDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }


  /*
  * segment values over a period
  */
  def segment(sqlContext: SQLContext,
              analysisPeriod: Int = 7,
              colList : Seq[String] = Seq(newUserColName, revUserColName),
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

    val (resultDF,outputColNames) = segmentG(userMetricsDF.get, sqlContext,aggregateOnCols, analysisPeriod,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, sortBy, verbose)

    printSegmentOutput(resultDF, outputColNames)
  }

  /*
  * segment any values over a period
  */
  def segmentAny(sqlContext: SQLContext,
                 analysisPeriod: Int = 7,
                 aggregateOnCol: AggregateOnCol = revUserAggCol,
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

    val (resultDF,outputColNames) = segmentG(userMetricsDF.get, sqlContext,aggregateOnCols, analysisPeriod,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, sortBy, verbose)

    printSegmentOutput(resultDF, outputColNames)
  }

  def getRankExpr(rankBy : String, rankOP : String, rank : Any) : Column = {

    val rankCol = rankBy.trim.toLowerCase match {
      case "time spent" => rankByTimeColName
      case "session count" => rankBySessionCountColName
      case "event count" => rankByEventCountColName
      case _	=> rankByTimeColName
    }

    val rankExpr = rankOP.toLowerCase match {
      case "eq" => new Column(rankCol).eqNullSafe(rank)
      case "geq" => new Column(rankCol).geq(rank)
      case "leq" => new Column(rankCol).leq(rank)
      case "lt" => new Column(rankCol).lt(rank)
      case "gt" => new Column(rankCol).gt(rank)
      case "like" => new Column(rankCol).like(rank.toString)
      case _ => new Column(rankCol).eqNullSafe(rank)
    }
    rankExpr
  }

  /*
  * Engagement metrics - average session count, time spent and events fired
  * ranked using ntiles on session count, time spent and events fired
   */
  def engagement(
                  sqlContext : SQLContext,
                  analysisPeriod : Int = 7,
                  forRevenueUsers : Boolean = false,
                  periodType : String = "daily",
                  sessionCountOnly : Boolean = false,
                  eventCountOnly : Boolean = false,
                  timeSpentOnly : Boolean = false,
                  movingAverage : Option[Int] = None,
                  segmentBy: Option[String] = None,
                  topNSegments : Int = 10,
                  filterOn: Option[String] = None,
                  filterOp: String = "eq",
                  filterVal: Option[Any] = None,
                  rank : Int = 1,
                  rankOp: String = "eq",
                  rankBy : Option[String] = Some("Time Spent"),
                  weekendOnly : Boolean = false,
                  verbose : Boolean = false
                  ) : Unit = {
    // Apply rank filter
    val startingDF = rankBy match {
      case None => userMetricsDF.get
      case rankDim => filterDF(userMetricsDF.get, getRankExpr(rankBy.get, rankOp, rank))
    }

    // Apply paid users filter
    val startingDFII = if (forRevenueUsers)
        filterDF(userMetricsDF.get, getFilterExpr(revUserColName, "eq", 1))
      else
        startingDF

    var aggregateOnCols = defaultEngagementAggrCols
    if(sessionCountOnly) aggregateOnCols = aggregateOnCols.filter(_.name == userSessionCountColName)
    if(eventCountOnly) aggregateOnCols = aggregateOnCols.filter(_.name == userEventCountColName)
    if(timeSpentOnly) aggregateOnCols = aggregateOnCols.filter(_.name == userTimeSpentColName)

    val (resultDF,outputColNames) = trendG(startingDFII, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }

  /*
  * behavior analysis
  * what did users do - video sessions, add-to-cart sessions, revenue sessions
  * exclusive behavior
 */
  def behavior(sqlContext : SQLContext,
               analysisPeriod : Int = 7,
               forRevenueUsers : Boolean = false,
               periodType : String = "daily",
               revenueSessionsOnly : Boolean = false,
               videoSessionsOnly : Boolean = false,
               cartSessionsOnly : Boolean = false,
               movingAverage : Option[Int] = None,
               segmentBy: Option[String] = None,
               topNSegments : Int = 10,
               filterOn: Option[String] = None,
               filterOp: String = "eq",
               filterVal: Option[Any] = None,
               rank : Int = 1,
               rankOp: String = "eq",
               rankBy : Option[String] = Some("Time Spent"),
               weekendOnly : Boolean = false,
               verbose : Boolean = false) : Unit = {

    // Now apply rank filter
    val startingDF = rankBy match {
      case None => userMetricsDF.get
      case rankDim => filterDF(userMetricsDF.get, getRankExpr(rankBy.get, rankOp, rank))
    }

    // Apply paid users filter
    val startingDFII = if (forRevenueUsers)
      filterDF(startingDF, getFilterExpr(revUserColName, "eq", 1))
    else
      startingDF

    var aggregateOnCols = defaultBehaviorAggrCols
    if(revenueSessionsOnly) aggregateOnCols = aggregateOnCols.filter(_.name == revSessionsColName)
    if(videoSessionsOnly) aggregateOnCols = aggregateOnCols.filter(_.name == videoSessionsColName)
    if(cartSessionsOnly) aggregateOnCols = aggregateOnCols.filter(_.name == cartSessionsColName)

    val (resultDF,outputColNames) = trendG(startingDFII, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }

  /*
   * inclusive behavior analysis - users did collection of events
  */
  def behaviorInclusive(sqlContext : SQLContext,
                        analysisPeriod : Int = 7,
                        forRevenueUsers : Boolean = false,
                        colList : Seq[String] = Seq(revSessionsColName, cartSessionsColName, videoSessionsColName),
                        periodType : String = "daily",
                        movingAverage : Option[Int] = None,
                        segmentBy: Option[String] = None,
                        topNSegments : Int = 10,
                        filterOn: Option[String] = None,
                        filterOp: String = "eq",
                        filterVal: Option[Any] = None,
                        rank : Int = 1,
                        rankOp: String = "eq",
                        rankBy : Option[String] = Some("Time Spent"),
                        weekendOnly : Boolean = false,
                        verbose : Boolean = false) : Unit = {

    val filterCond = colList match { // all three, two or one
      case Seq(a, b, c) => new Column(a).eqNullSafe(1).and(new Column(b).eqNullSafe(1)).and(new Column(c).eqNullSafe(1))
      case Seq(a, b) => new Column(a).eqNullSafe(1).and(new Column(b).eqNullSafe(1))
      case Seq(a) => new Column(a).eqNullSafe(1)
      case _ => new Column(revSessionsColName).eqNullSafe(1)
    }
    val startingDF = sessionMetricsDF.get.filter(filterCond)

    // Apply paid users filter
    val startingDFII = if (forRevenueUsers)
      filterDF(userMetricsDF.get, getFilterExpr(revUserColName, "eq", 1))
    else
      startingDF

    val aggregateOnCols = Seq(userAggCol)
      /* defaultBehaviorAggrCols.map{ case col : AggregateOnCol =>
      if(colList.contains(col.name)) col else null}.toList */

    val (resultDF,outputColNames) = trendG(startingDFII, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    printTrendOutput(resultDF, outputColNames)
  }


  /*
  * Cohort Conversion
   */
  def cohort(
                  sqlContext : SQLContext,
                  startDaysAgo : Int = 14,
                  daysToConversionLimit : Int = 7,
                  filterOn: Option[String] = None,
                  filterOp: String = "eq",
                  filterVal: Option[Any] = None,
                  aggFilterOn : Option[String] = None,
                  aggFilterOp: String = "eq",
                  aggFilterVal: Option[Any] = None,
                  weekendOnly : Boolean = false,
                  verbose : Boolean = false
                  ) : Unit = {

    val aggregateOnCols = Seq(allUserAggCol, revUserAggCol, convRateAggCol)

    // First seen at expressions
    val firstSeenAtExpr = getDateExprFromMillis(firstSeentAtColName)
    val firstSeenAtDayExpr = firstSeenAtExpr dayOfYear

    val cohortStartDateExpr = getPriorDateExp(startDaysAgo)

    // where clause - filter out
    val isAfter = firstSeenAtExpr >= cohortStartDateExpr
    val period = firstSeenAtExpr to firstSeenAtExpr + Period.days(daysToConversionLimit)
    val inPeriod = period containsE dateExpr
    val daysToConversionExpr = Subtract(doyExpr, firstSeenAtDayExpr)

    val filterExpr = new Column(And(isAfter, inPeriod))
    val filteredDF = if(filterOn.isDefined && filterVal.isDefined)
      filterDF(userMetricsDF.get, filterExpr, getFilterExpr(filterOn.get, filterOp, filterVal.get))
    else
      filterDF(userMetricsDF.get, filterExpr)

    // Group by clause
    val groupByCols = dateColNames.map(new Column(_)) :+ new Column(daysToConversionExpr).alias("days_to_conversion")
    val groupedDF = filteredDF.groupBy(groupByCols: _*)

    // aggregate
    val aggrMap = aggregateOnCols.map(ac => (ac.colExpr))
    val aggrDF = aggDF(groupedDF, aggrMap)

    // apply aggregate filter if any
    val aggrDFII = aggFilterOn match {
      case None => aggrDF
      case rankDim => filterDF(aggrDF, getFilterExpr(aggFilterOn.get, aggFilterOp, aggFilterVal.getOrElse(10)))
    }
    val orderColNames = dateColNames :+ "days_to_conversion"
    // order filter
    val resultDF = orderDF(aggrDFII, orderColNames)

    val sqlQuery =
      date"""
        |select sd_year, sd_month, sd_day, ($doyExpr - $firstSeenAtDayExpr) as days_to_conversion,
        |sum(sd_is_revenue_user) as revenue_users, count(user_id) as all_users
        |from sd_user_metricsII where $isAfter and $inPeriod
        |group by sd_year, sd_month, sd_day, ($doyExpr - $firstSeenAtDayExpr)
        |having revenue_users > 0  order by sd_year, sd_month, sd_day, days_to_conversion
      """.stripMargin

    if(verbose)
      println(sqlQuery)
    val outputColNames = Seq("Period", "DaysToConversion") ++ aggregateOnCols.map(ac => ac.alias)
    printTrendOutput(resultDF,  outputColNames)
  }

}