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

import com.github.nscala_time.time.Imports._
import com.sparklinedata.analytics.utils.DataframeUtils._
import org.apache.spark.sql.catalyst.analysis.{UnresolvedAttribute, UnresolvedFunction}
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.{Column, DataFrame, SQLContext}
import org.joda.time.DateTime
import org.sparklinedata.spark.dateTime.Functions
import org.sparklinedata.spark.dateTime.dsl.DateExpression
import org.sparklinedata.spark.dateTime.dsl.expressions._

/**
 * Created by Jitender on 8/7/15.
 */

trait AggregateCol {

  val name : String
  val alias : String
  def colExpr : Column

  def maColName : String = name + "_ma"
  def maColAlias : String = alias + "_MA"
  def maColExpr : Column = org.apache.spark.sql.functions.avg(colExpr)
}

case class AggregateOnColExpr(name : String, alias : String, col : Column) extends AggregateCol{
  def colExpr : Column = col.alias(alias)
}

case class AggregateOnCol (name : String, alias : String, func : String, cond : Any = None) extends AggregateCol {
  self =>

  def colExprWOAlias : Column = func match {
    case "count" => org.apache.spark.sql.functions.count(name)
    case "countd" => org.apache.spark.sql.functions.countDistinct(name)
    case "countIf" => org.apache.spark.sql.functions.sum(
      org.apache.spark.sql.functions.when(new Column(name).equalTo(cond), 1).otherwise(0))
    case "countIfLike" => org.apache.spark.sql.functions.count(new Column(name).like(cond.toString))
    case "countNotNull" => {//count not null
      org.apache.spark.sql.functions.sum(
        org.apache.spark.sql.functions.when(new Column(name).isNotNull === true, 1).otherwise(0))
    }
    case "sum" => org.apache.spark.sql.functions.sum(name)
    case "sumd" => org.apache.spark.sql.functions.sumDistinct(name)
    case "avg" => {
      //org.apache.spark.sql.functions.avg(name)
      new Column(
        UnresolvedFunction("round", Seq(
          Average(
            UnresolvedAttribute(name)
          ),
          Literal(2))))
    }
    case "max" => org.apache.spark.sql.functions.max(name)
    case "min" => org.apache.spark.sql.functions.min(name)
    case "first" => org.apache.spark.sql.functions.first(name)
    case "last" => org.apache.spark.sql.functions.last(name)
    case _ => org.apache.spark.sql.functions.count(name)
  }

  def colExpr : Column = colExprWOAlias.alias(alias)

  def getMap : (String, String) = (name -> func)
}

trait BaseAnalytics {

  val analyticsBucketURL = "s3n://YOUR_BUCKET/WITH_CUBES/"
  val dateTimeAttrName = "utc_time"
  val dayAttrName = "sd_day"
  val monthAttrName = "sd_month"
  val yearAttrName = "sd_year"
  val weekAttrName = "sd_week"

  val dateColNames =  Seq(yearAttrName, monthAttrName, dayAttrName)
  val weekColNames =  Seq(yearAttrName, monthAttrName, weekAttrName)
  val monthColNames =  Seq(yearAttrName, monthAttrName)

  var eventsDF : Option[DataFrame] = None
  var sessionMetricsDF : Option[DataFrame] = None
  var userMetricsDF : Option[DataFrame] = None
  var convRateDF : Option[DataFrame] = None
  var sdProducts :  Option[DataFrame] = None
  var goalsDF :  Option[DataFrame] = None

  def cubeBasePath : String = analyticsBucketURL

  def loadDateFunctions(sqlContext : SQLContext) : Unit = Functions.register(sqlContext)

  def beforeAll(sqlContext : SQLContext) : Unit = loadDateFunctions(sqlContext)

  def groupByDateCols : Seq[Column] =  dateColNames.map(new Column(_)) // group by coulmns for time series

  def dateExpr : DateExpression = dateTime(UnresolvedAttribute(dateTimeAttrName))

  def dateInMillisExpr : Expression = dateExpr millis

  def doyExpr : Expression = dateExpr dayOfYear

  val woyExpr = dateExpr weekOfWeekyear


  def weekendOnlyExpr : Expression = Or(EqualTo((dateExpr dayOfWeekName), Literal("Saturday")),
    EqualTo((dateExpr dayOfWeekName), Literal("Sunday")))
  //def weekendOnlyExpr : Expression = ((dateExpr dayOfWeekName) === "Saturday") || ((dateExpr dayOfWeekName) === "Sunday")

  //current date is always one day behind
  //def currentDate : DateTime = new DateTime(org.joda.time.DateTimeZone.UTC).minusDays(1).withTimeAtStartOfDay()
  // fix the current date to Sep 1st
  def currentDate : DateTime = new DateTime(2015, 9, 1, 0, 0, org.joda.time.DateTimeZone.UTC).minusDays(1).withTimeAtStartOfDay()

  def currentDateInMillis(currentDate : DateTime) : Long = currentDate.getMillis()

  def getDateExprFromMillis (attrName : String) : DateExpression =
    dateTimeFromEpoch(UnresolvedAttribute(attrName))

  /* def getPriorDateExp(daysAgo : Integer = 7) : DateExpression = dateTimeFromEpoch(
    Literal(new DateTime(org.joda.time.DateTimeZone.UTC).minusDays(daysAgo).withTimeAtStartOfDay().getMillis())) */
  def getPriorDateExp(daysAgo : Integer = 7) : DateExpression = dateTimeFromEpoch(
    Literal(new DateTime(2015, 9, 1, 0, 0, org.joda.time.DateTimeZone.UTC).minusDays(daysAgo).withTimeAtStartOfDay().getMillis()))


  def getPeriodExpr(dt : DateExpression, endDate : DateTime, analysisPeriod: Int, periodType : String = "day"
                     )  : UnresolvedFunction = {
    val periodBack = periodType match {
      case "daily" => Period.days(analysisPeriod)
      case "weekly" => Period.weeks(analysisPeriod)
      case "monthly" => Period.months(analysisPeriod)
      case _ => Period.days(analysisPeriod)
    }
    val period = endDate - periodBack to  endDate + 1.day
    period containsE dt
  }

  def currentPeriodExpr(currentDate : DateTime, dt : DateExpression) : UnresolvedFunction
  = getPeriodExpr(dt, currentDate, 0) //from beginning of the day in mid-night to the next-day midnight

  def getFilterExpr(filterOn : String, filterOp : String, filterVal : Any) : Column = {
    filterOp.toLowerCase match {
      case "eq" => new Column(filterOn).eqNullSafe(filterVal)
      case "geq" => new Column(filterOn).geq(filterVal)
      case "leq" => new Column(filterOn).leq(filterVal)
      case "lt" => new Column(filterOn).lt(filterVal)
      case "gt" => new Column(filterOn).gt(filterVal)
      case "like" => new Column(filterOn).like(filterVal.toString)
      case _ => new Column(filterOn).eqNullSafe(filterVal)
    }
  }


  /*
  * Load cube for event analysis
  */
  def loadCubeInMemory(sqlContext : SQLContext, cubeName : String) : Option[DataFrame] = {
    val s3Bucket = List(cubeBasePath,cubeName).mkString("/")
    try {
      print(s"Loading cube $cubeName...")
      val df = sqlContext.read.parquet(s3Bucket)
      //df.cache()
      df.registerTempTable(cubeName)
      print(s"\t...cube loaded.")
      Some(df)
    }
    catch {
      case e: Throwable => {
        println(s"""Oops, technology issues are utterly unpredictable.
          I can't resolve this error at this moment. Please contact us. Issue: ${e.getMessage}""")
        None
      }
    }
  }

  def loadCube(sqlContext : SQLContext, lookbackDays: Int = 60) : Unit
}

/*
 * segment analysis
 */
trait SegmentAnalysis  extends BaseAnalytics {

  protected def segmentG(
                          startingDF: DataFrame,
                          sqlContext: SQLContext,
                          aggregateOnCols: Seq[AggregateCol],
                          analysisPeriod: Int,
                          segmentBy: Option[String] = None,
                          topNSegments : Int = 10,
                          filterOn: Option[String] = None,
                          filterOp: String = "eq",
                          filterVal: Option[Any] = None,
                          sortBy: Seq[String] = Seq(),
                          verbose: Boolean = false): (DataFrame, Seq[String]) = {

    val inAnalysisPeriodExpr = getPeriodExpr(dateExpr, currentDate, analysisPeriod, "day")
    segmentOverPeriod(startingDF, sqlContext, aggregateOnCols, inAnalysisPeriodExpr, segmentBy, topNSegments,
    filterOn, filterOp, filterVal, sortBy, verbose)
  }

  protected def segmentOverPeriod(
                          startingDF: DataFrame,
                          sqlContext: SQLContext,
                          aggregateOnCols: Seq[AggregateCol],
                          analysisPeriodExpr : Expression,
                          segmentBy: Option[String] = None,
                          topNSegments : Int = 10,
                          filterOn: Option[String] = None,
                          filterOp: String = "eq",
                          filterVal: Option[Any] = None,
                          sortBy: Seq[String] = Seq(),
                          verbose: Boolean = false): (DataFrame, Seq[String]) = {

    val compositeFilterExpr = if (filterOn.isDefined && filterVal.isDefined) {
      val filterExpr = getFilterExpr(filterOn.get, filterOp, filterVal.get)
      Seq(new Column(analysisPeriodExpr), filterExpr, new Column(segmentBy.get).isNotNull)
    } else {
      Seq(new Column(analysisPeriodExpr), new Column(segmentBy.get).isNotNull)
    }

    val groupByColNames = Seq(segmentBy.get)
    val aggrColNames = aggregateOnCols.map(ac => ac.alias)
    val aggrMap = aggregateOnCols.map(ac => (ac.colExpr))
    val outputColNames = Seq("Segment") ++ aggrColNames // for final output
    val orderByColNames = if (sortBy.length > 0) sortBy else aggrColNames

    //filter for period, filter for null, groupby, aggr, orderby, limit step
    val resultDF =
      orderDF(
        aggDF(
          groupByDF(
            filterDF(
              startingDF,
              compositeFilterExpr: _*
            ),
            groupByColNames
          ),
          aggrMap
        ),
        orderByColNames,
        "desc"
      ).limit(topNSegments)
    (resultDF, outputColNames)
  }

    /*
  * Output results
   */
  protected def printSegmentOutput(resultDF: DataFrame, outputColNames : Seq[String]): Unit = {
    val sqlResults = resultDF.map {
      //Implementing variable length argument to avoid a boat load of row case classes
      case org.apache.spark.sql.Row (vals@_*) => {
        vals.mkString ("\t ")
      }
    }.collect ()
    //Return result as a table
    val headerColsSt = outputColNames.mkString ("", "\t", "\n")
    println (s"%table $headerColsSt" + sqlResults.mkString ("\n") )
  }

}


/*
 * trend analysis
 */
trait TrendAnalysis extends BaseAnalytics{

  protected def trendG(
                        startingDF: DataFrame,
                        sqlContext: SQLContext,
                        aggregateOnCols: Seq[AggregateCol],
                        analysisPeriod: Int = 7,
                        periodType: String = "daily",
                        movingAverage: Option[Int] = None,
                        segmentBy: Option[String] = None,
                        topNSegments : Int = 10,
                        filterOn: Option[String] = None,
                        filterOp: String = "eq",
                        filterVal: Option[Any] = None,
                        weekendOnly : Boolean = false,
                        verbose: Boolean = false): (DataFrame, Seq[String]) = {

    var selectColNames = dateColNames // for final output
    val inAnalysisPeriodExpr = getPeriodExpr(dateExpr, currentDate, analysisPeriod, periodType.toLowerCase)
    val inAnalysisWMAPeriodExpr = if(movingAverage.isDefined)
        getPeriodExpr(dateExpr, currentDate, analysisPeriod + movingAverage.get, periodType.toLowerCase)
      else
        inAnalysisPeriodExpr

    // Filter Step
    // if moving average is present - filter on time period
    // then if filterOn and filterVal is present - filter on val too
    val filteredDF = if(filterOn.isDefined && filterVal.isDefined)
        filterDF(startingDF,
          new Column(inAnalysisWMAPeriodExpr), getFilterExpr(filterOn.get, filterOp, filterVal.get))
      else
        filterDF(startingDF, inAnalysisWMAPeriodExpr)

    // group-by and filter on date expression
    val groupByColNames =
      periodType.toLowerCase match {
        case "weekly" => weekColNames
        case "monthly" => monthColNames
        case "daily" => dateColNames
        //case _ => println(s"$periodType not supported. Supported values are: daily, weekly, monthly")
      }

    // GroupBy step
    // will remove segments called nulls
    val groupedDF = segmentBy match {
      case None => groupByDF(filteredDF, groupByColNames)
      case dim => {
        selectColNames = selectColNames :+ s"${segmentBy.get}"
        val extGroupByCols = groupByColNames :+ segmentBy.get
        groupByDF(
          filterDF(
            filteredDF,
            new Column(segmentBy.get).isNotNull
          ),
          extGroupByCols
        )
      }
    }

    // Aggregate Step
    var aggrMap = movingAverage match {
      case None => {
        selectColNames = selectColNames ++ aggregateOnCols.map(ac => ac.alias)
        aggregateOnCols.map(ac => (ac.colExpr))
      }
      case ma => {
        selectColNames = selectColNames ++ aggregateOnCols.map(ac => List(ac.alias, ac.maColAlias)).flatten
        // window function for moving average
        val w = Window.orderBy((groupByColNames.map(new Column(_))): _*).rowsBetween(-ma.get, 0)
        aggregateOnCols.map(ac => List(ac.colExpr, ac.maColExpr.over(w).alias(ac.maColAlias))).flatten
      }
    }
    // add utc_time and sd_day (if period is not daily) to the list of aggregated columns
    if (periodType != "daily") aggrMap = aggrMap :+ org.apache.spark.sql.functions.first(dayAttrName).as(dayAttrName)
    aggrMap = aggrMap :+ org.apache.spark.sql.functions.first(dateTimeAttrName).as(dateTimeAttrName)

    val aggrDF = aggDF(groupedDF, aggrMap)

    //Second level of filter step
    val filteredDFII = weekendOnly match {
      case false => filterDF(aggrDF, inAnalysisPeriodExpr)
      case true => filterDF(aggrDF, inAnalysisPeriodExpr, weekendOnlyExpr)
    }

    // Trimming step
    //Keep useful cols and drop others
    //val slimmerDF = rearrangeDF(filteredDFII, dayAttrName, Seq(weekAttrName, dayAttrName, dateTimeAttrName))

    // Final step - ranking and order
    // ranking - if segmented, limit number of records per segment to topN
    // order by date asc
    val resultDF = if(segmentBy.isDefined) {
      val partitionCols = groupByColNames.map(new Column(_))
      val sortCols = aggregateOnCols.map(ac => new Column(ac.alias).desc)
      val wpb = Window.partitionBy(partitionCols: _*)
      val wob = wpb.orderBy(sortCols: _*)
      //get rank col
      val rankCol = org.apache.spark.sql.functions.rank().over(wob)
      filteredDFII.withColumn("rank", rankCol).filter(s"rank < $topNSegments").select(selectColNames.head, selectColNames.tail: _*)
    } else {
      orderDF(filteredDFII.select(selectColNames.head, selectColNames.tail: _*), dateColNames)
    }
    val outputColNames = "Period" +: selectColNames.diff(dateColNames)
    if (verbose) println(resultDF.queryExecution.logical)
    (resultDF,outputColNames)
  }

  /*
  * Output results
   */
  protected def printTrendOutput(resultDF: DataFrame, outputColNames : Seq[String]): Unit = {
    val sqlResults = resultDF.map {
      //Implementing variable length argument to avoid a boat load of row case classes
      case org.apache.spark.sql.Row (sd_year: Int, sd_month: Int, sd_day: Int, vals@_*) => {
        sd_year + "-" + sd_month + "-" + sd_day + "\t" + vals.mkString ("\t ")
      }
    }.collect()

    //Return result as a table
    val headerColsSt = outputColNames.mkString ("", "\t", "\n")
    println (s"%table $headerColsSt" + sqlResults.mkString ("\n") )
  }

}
