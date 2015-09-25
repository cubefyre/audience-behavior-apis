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

import org.sparklinedata.analytics.utils.DataframeUtils._
import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute
import org.apache.spark.sql.catalyst.plans.logical.RepartitionByExpression
import org.apache.spark.sql.{Column, SQLContext}

/**
 * Created by Jitender on 8/20/15.
 */
object Conversions extends BaseAnalytics with TrendAnalysis with SegmentAnalysis {
  val conversionToColName: String = "sd_rev_user"
  val conversionFromColName: String = "sd_all_user"

  val convFromAggCol = AggregateOnCol(conversionFromColName, "Total", "sum")
  val convToAggCol = AggregateOnCol(conversionToColName, "Converted", "sum")
  val convRateAggColExpr = convToAggCol.colExprWOAlias.divide(convFromAggCol.colExprWOAlias).multiply(100)
  val convRateAggCol = AggregateOnColExpr("sd_conv_rate", "ConversionRate", convRateAggColExpr)

  def defaultAggrCols: Seq[AggregateOnCol] = {
    Seq(convFromAggCol, convToAggCol)
  }

  /*
  * Load cube for event analysis
  */
  def loadCube(sqlContext : SQLContext, lookbackDays: Int = 60) : Unit = {
    beforeAll(sqlContext)
    val df = loadCubeInMemory(sqlContext, "sd_conversion_rate")
    convRateDF = Some(df.get.withColumn(weekAttrName, new Column(woyExpr)).cache())
    println(s"\tCube has ${convRateDF.get.count()} records.")
   }

  /*
   * trend analysis
  */
  def trend(
             sqlContext : SQLContext,
             analysisPeriod : Int = 7,
             includeDefaults : Boolean = true,
             periodType : String = "daily",
             movingAverage : Option[Int] = None,
             segmentBy: Option[String] = None,
             topNSegments : Int = 10,
             filterOn: Option[String] = None,
             filterOp: String = "eq",
             filterVal: Option[Any] = None,
             weekendOnly : Boolean = false,
             aggFilterOn : Option[String] = None,
             aggFilterOp: String = "eq",
             aggFilterVal: Option[Any] = None,
             verbose : Boolean = false) : Unit = {
    val aggregateOnCols : Seq [AggregateCol] = if (includeDefaults)
      defaultAggrCols ++ Seq(convRateAggCol)
    else
      Seq(convRateAggCol)

    val (resultDF,outputColNames) = trendG(convRateDF.get, sqlContext, aggregateOnCols, analysisPeriod, periodType, movingAverage,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, weekendOnly, verbose)

    val finalDF = aggFilterOn match {
      case None => resultDF
      case rankDim => filterDF(resultDF, getFilterExpr(aggFilterOn.get, aggFilterOp, aggFilterVal.getOrElse(10)))
    }

    printTrendOutput(finalDF, outputColNames)
  }

  /*
  * segment multiple cols over a period
  */
  def segment(
               sqlContext: SQLContext,
               analysisPeriod: Int = 7,
               includeDefaults : Boolean = true,
               segmentBy: Option[String] = None,
               topNSegments : Int = 10,
               filterOn: Option[String] = None,
               filterOp: String = "eq",
               filterVal: Option[Any] = None,
               sortBy : Seq[String] = Seq(),
               aggFilterOn : Option[String] = None,
               aggFilterOp: String = "eq",
               aggFilterVal: Option[Any] = None,
               verbose: Boolean = false
               ) : Unit = {

    val aggregateOnCols : Seq [AggregateCol] = if (includeDefaults)
      defaultAggrCols ++ Seq(convRateAggCol)
    else
      Seq(convRateAggCol)

    val (resultDF,outputColNames) = segmentG(convRateDF.get, sqlContext, aggregateOnCols, analysisPeriod,
      segmentBy, topNSegments, filterOn, filterOp, filterVal, sortBy, verbose)

    val finalDF = aggFilterOn match {
      case None => resultDF
      case rankDim => filterDF(resultDF, getFilterExpr(aggFilterOn.get, aggFilterOp, aggFilterVal.getOrElse(10)))
    }
    printSegmentOutput(finalDF, outputColNames)
  }
}
