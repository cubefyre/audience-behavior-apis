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

/**
 * Created by Jitender on 8/11/15.
 */
object Utils {

  def getCaseClassParams(cc: AnyRef) =
    (Map[String, Any]() /: cc.getClass.getDeclaredFields) {(a, f) =>
      f.setAccessible(true)
      a + (f.getName -> f.get(cc))
    }

  //  getCaseClassParams(primaryAggCol) = Map(name -> sessionIdColName, alias -> Sessions, func -> count, $outer -> $iwC$$iwC@507418f)
  // P21 (*) Insert an element at a given position into a list.
  //     Example:
  //     scala> insertAt('new, 1, List('a, 'b, 'c, 'd))
  //     res0: List[Symbol] = List('a, 'new, 'b, 'c, 'd)
  def insertAt[A](e: A, n: Int, ls: List[A]): List[A] = ls.splitAt(n) match {
    case (pre, post) => pre ::: e :: post
  }

}
