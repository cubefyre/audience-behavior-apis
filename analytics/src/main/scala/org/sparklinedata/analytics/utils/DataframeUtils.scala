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

 package org.sparklinedata.analytics.utils

import org.apache.spark.sql.catalyst.expressions.{And, Expression}
import org.apache.spark.sql.{GroupedData, Column, DataFrame}
import org.apache.spark.sql.catalyst.analysis.UnresolvedFunction
import org.sparklinedata.analytics.utils.Utils._
/**
 * Created by Jitender on 8/11/15.
 */
// data frame related methods
object DataframeUtils {

  // filter row based on threshold on numeric columns
  /*
  def limitDF(df: DataFrame, thresholdVal : Int, filterCols : Set[String]) : DataFrame = {
    var dTypes = Map(df.dtypes:_*)
    dTypes = dTypes.filterKeys { filterCols.contains(_) == false }
    val cols = dTypes.map {
      case (name, "IntegerType") => new Column(name).gt(thresholdVal)
      case (name, "LongType") => new Column(name).gt(thresholdVal)
      case (name, "DoubleType") => new Column(name).gt(thresholdVal)
    }.toList
    cols.length match {
      case 1 => df.filter(cols(0))
      case 2 => df.filter(cols(0).and(cols(1)))
      case 3 => df.filter(cols(0).and(cols(1)).and(cols(2)))
      case _ => df.filter(cols(0))
    }
  }
   */

  // filter row based on threshold on aggregated columns
  def limitDF(df: DataFrame, thresholdVal : Int, filterOnCols : Seq[String]) : DataFrame = {
    val cols = filterOnCols.map(new Column(_).gt(thresholdVal))
    cols.length match {
      case 1 => df.filter(cols(0))
      case 2 => df.filter(cols(0).and(cols(1)))
      case 3 => df.filter(cols(0).and(cols(1)).and(cols(2)))
      case 4 => df.filter(cols(0).and(cols(1)).and(cols(2)).and(cols(3)))
      case _ => df.filter(cols(0))
    }
  }


  def filterDF(df : DataFrame, filterCond: String) : DataFrame = df.filter(filterCond)

  def filterDF(df : DataFrame, filterCols : Column*) : DataFrame = {
    if (filterCols.length == 0)
      df
    else
      filterDF(df.filter(filterCols.head), filterCols.tail: _*)
  }
  // to avoid type earsure - added dummy implicit
  def filterDF(df : DataFrame, filterExprs : Expression*) (implicit d: DummyImplicit) : DataFrame = {
    if (filterExprs.length == 0)
      df
    else
      filterDF(df.filter(new Column(filterExprs.head)), filterExprs.tail: _*)
  }

  def groupByDF(df : DataFrame, cols: Seq[String]) : GroupedData = df.groupBy((cols.map(new Column(_))): _*)

  //private def groupByDF(df : DataFrame, cols: Seq[Column]) : GroupedData = df.groupBy(cols: _*)

  def aggDF(df : GroupedData, cols: Map[String, String]) : DataFrame = df.agg(cols)

  def aggDF(df : GroupedData, cols: Seq[Column]) : DataFrame = df.agg(cols.head, cols.tail:_*)


  def orderDF(df : DataFrame, cols : Seq[String], order : String = "asc") : DataFrame = {
    order match {
      case "desc" => df.orderBy((cols.map(new Column(_).desc)): _*)
      case _ => df.orderBy((cols.map(new Column(_))): _*)
    }
  }

  def orderDF(df : DataFrame, cols : Map[String, String]) : DataFrame = {
    val colOrder = cols.map{
      case (name, "asc") => new Column(name)
      case (name, "desc") => new Column(name).desc
    }.toList
    df.orderBy(colOrder: _*)
  }

  def renameDFCols(df : DataFrame, cols : Seq[String]) : DataFrame = df.toDF(cols: _*)

  def rearrangeDF(df: DataFrame, insertCol : String, removeCols : Seq[String]) : DataFrame = {
    val selectNames = insertAt(insertCol, 2, df.columns.toList.filterNot(removeCols.toSet))
    df.select(selectNames.head, selectNames.tail:_*)
  }


}
