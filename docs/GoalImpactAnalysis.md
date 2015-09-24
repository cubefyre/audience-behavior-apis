# Goal-Impact Analysis  

For this data-cube, a goal event is a conversion event which is identified as "Completed Order". Few events and pages have being tagged as impact events and pages.  The goal of this analysis is to understand what events and pages drive more users, revenues and relatively rank them. Example of impact events are "watched video", "reviewed the 30-day-money-back-guarantee" etc.

######Use this data product to:
 - understand the impact of events and pages on goals
    - see metrics such as sessions-to-goal, time-to-goal, days-to-goal
- understand the influence of impact pages on users and revenues
- Preform multi-touch and last-touch attribution for impact events and pages

> Impact Events Ranked by Metrics

![Image](img/IA-1.png?raw=true)

>  Users & Revenues Influeced by Events and Pages

![Image](img/IA-3.png?raw=true)

> Top Impact Events

- Understand the effect of impact pages and events on a segment by applying filter

![Image](img/IA-2.png?raw=true)

- Impact Pages by MultiTocuh and LastTouch Attribution

![Image](img/IA-4.png?raw=true)

> Extend Development in Scala or Python 

```
import com.github.nscala_time.time.Imports._

import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.analysis.UnresolvedFunction
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.dsl.expressions._

import org.sparklinedata.spark.dateTime.dsl.expressions._
import org.sparklinedata.spark.dateTime.dsl._
//val revAttrToImpactEventFunction = UnresolvedWindowFunction("avg", Seq("productRevenueAttrExpr"))

// register all date functions 
//Functions.register(sqlContext)

val dt = dateTime('sd_impact_date_time_utc)
//val sqlQ = date"select count(distinct sd_impact_session_id) from sd_goals group by sd_goal_session_id, sd_impact_event_name"
//count(sd_impact_session_id) over (order by $dt rows between CURRENT ROW and UNBOUNDED FOLLOWING) 
val df = sql("select sd_goal_session_id, sd_product_revenue, sd_impact_session_id, sd_impact_date_time_utc, rank() over (partition by sd_goal_session_id order by sd_impact_date_time_utc) as rank from sd_goals")
df.take(10).foreach(println)

println(df.queryExecution.logical)
```