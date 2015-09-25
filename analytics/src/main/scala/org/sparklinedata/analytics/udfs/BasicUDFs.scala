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
package org.sparklinedata.analytics.udfs

import scala.math._
import org.apache.spark.sql.functions.udf

/**
 * Created by Jitender on 8/12/15.
 */
object BasicUDFs {


  val toInt    = udf[Int, String]( _.toInt)
  val toDouble = udf[Double, String]( _.toDouble)

  val toTime = (t: String) => t.substring(11, 19)
  val toDate = (t: String) => t.split("T")(0) //(t: String) => t.substring(0, 10)
  val toDateUDF = udf(toDate)
  val toTimeUDF = udf(toTime)
  val toHour   = udf((t: String) => "%04d".format(t.toInt).take(2).toInt )
  val toAbs = udf((t: Double) => abs(t))
  val toRound = udf((t: Double) => round(t))
}
