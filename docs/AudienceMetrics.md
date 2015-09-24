# Audience (User) Metrics

Use this data product to peform:
 - trend analysis on all users, new vs returning users, revenue users
    - apply filter to identify granular trends, go deep in your data
    - segment trends by geo, host, campaign and more
    - segments and filters can be combined
 - Group by analysis for top-n over a period 
 - Get engagement and behavioral metrics on users -
 - Get all metrics by rank (ntiles)

Ranking: This data cube has pre-computed daily ranks for users based on the following ranking dimensions:
  - "time spent" => "sd_rank_by_time_spent"
  - "session count" => "sd_rank_by_session_count"
  - "event count" => "sd_rank_by_event_count
  
> Trend Analysis

- Get daily, weekly, monthly trends for Unique Users, Revenue Users, New vs Returning Users
- Add moving average to understand long-term trends and deviations from the norm
- Analyze new vs returning users trend - determine what percent of users are return users

``` 
Example: Daily Trend Of Revenue Users With 7 DMA
Users.trend(sqlContext, 30,  movingAverage=Some(7), includeRevenueUsersTrend=true, includeUsersTrend=false)

Example: Revenue Users Breakdown By New And Returning Users
Users.trend(sqlContext, 30, includeNewUsersTrend=true, includeReturningUsersTrend=true, includeUsersTrend=false, filterOn=Some("sd_is_revenue_user"), filterVal=Some(1))
```

> Segmentation And Filtering

- Supply any dimension for segmentation and use the threshold value to limit the output records
- Find trend for a specific segment (for example - revenue users when country is United States or browser is chrome)
- Supply segment name and segment value to filter trends
- To get filter values, run a SQL query

```
Example: Weekly Revenue Users By Campaign (Period - 4 Weeks)
Users.trend(sqlContext, 4, periodType="weekly", segmentBy=Some("campaign_name"), includeRevenueUsersTrend=true, includeUsersTrend=false, topNSegments=5)
```

> Top N (Grouping)

- Over the last month, which cities users come from?
- What browsers paid users like to use?
- Top 10 hosts for new users
- Apply sorting and filtering

``` 
Example: Top 10 Sources For New And Revenue Users
Users.segment(sqlContext, 15, colList=Seq("sd_is_new_user", "sd_is_revenue_user"), segmentBy=Some("sd_source"), topNSegments=10)
```

> User Engagement Metrics

- Engagements are defined as the key activites users performed with any of the digital assets - web, mobile app or a social interaction:
    - amount of time user spent
    - number of key events fired during the engagment and 
    - number of sessions user opened to engage.
- Get enagement metrics for top ranked users, paid users or apply any other filters.

![Image](img/EngagementMetrics.png?raw=true)

> User Behavior Metrics (UBM)

- Behavior is defined as the actions perforemd while engaging with any of the digital assets - web, mobile app or a social interaction. Examples include: 
    - opend a video, 
    - read a review, 
    - added item to the cart or 
    - checked out / purchased an item.
- Get behavior metrics for top ranked users, paid users or apply any other filters

> Extend Analysis Using SQL, Scala Or Python

Flexibility to run Spark SQL and write Scala / Python code against a Spark cluster
- Cube is avaialble to you as dataframe in Scala and as a temporary table in the cluster for SQL access
- Run SQL queries directly against the curated data
- Write your own scala or python code and execute against the spark cluster

```
Run Spark And HIVE SQL
%sql
---desc sd_user_metricsII
--select sd_year, sd_month, sd_day, count(distinct user_id), sum(sd_is_revenue_user), sum(sd_is_new_user) from sd_user_metricsII where dateIsAfterOrEqual(dateTimeFromEpoch(`sd_first_seen_at`),dateTimeFromEpoch(1439510400000)) group by sd_year, sd_month, sd_day
---select concat(sd_year, '-', sd_month, '-', sd_day) as date , count(sd_is_new_user) as all_users from sd_user_metricsII where sd_country_name='United States' group by sd_year, sd_month, sd_day
--- Users, New and Returning
select sd_year, sd_month, sd_day, count(distinct user_id), sum(sd_is_new_user) as nu, sum(CASE when sd_is_new_user=0 then 1 else 0 end) as ru  from sd_user_metricsII group by sd_year, sd_month, sd_day
---select concat(sd_year, '-', sd_month, '-', sd_day) as dimension , count(distinct user_id) as all_users, ROUND(AVG(count(distinct user_id)) OVER(ORDER BY sd_year, sd_month, sd_day ROWS BETWEEN 6 PRECEDING AND CURRENT ROW),2) as moving_average  from sd_user_metricsII where intervalContainsDateTime(intervalFromStr("2015-07-29T00:00:00.000Z/2015-08-06T00:00:00.000Z"),dateTime(`utc_time`)) group by sd_year, sd_month, sd_day 
```

```
Scala

import com.github.nscala_time.time.Imports._

import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.analysis.UnresolvedFunction
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.dsl.expressions._

import org.sparklinedata.spark.dateTime.dsl.expressions._
import org.sparklinedata.spark.dateTime.dsl._

// Get engagement metrics for the past 7 days using SQL against a temp table
val inAnalysisPeriodExpr = Users.getPeriodExpr(Users.dateExpr, Users.currentDate, 7)
/*
val inAnalysisPeriodExpr = Users.getPeriodExpr(Users.dateExpr, Users.currentDate, 7)
val sqlSt = date"select concat(sd_year, '-', sd_month, '-', sd_day) as date, round(avg(sd_session_count),2) as average_sessions, round(avg(sd_event_count),2) as average_events, abs(round(avg(sd_time_spent)/1000,2)) as average_time_spent from sd_user_metricsII where $inAnalysisPeriodExpr group by sd_year, sd_month, sd_day"
val sqlResults = sql(sqlSt)
//println(sqlResults.queryExecution.logical)
sqlResults.take(20).foreach(println)
*/

// new vs returning users using spark dataframe functions
/*
val userColExpr =  org.apache.spark.sql.functions.count(new Column("user_id")).alias("Users")
val newUserColExpr =  org.apache.spark.sql.functions.sum(new Column("sd_is_new_user")).alias("New User")
val retUserColExpr = org.apache.spark.sql.functions.sum(org.apache.spark.sql.functions.when(new Column("sd_is_new_user").equalTo(0), 1).otherwise(0)).alias("Returning User")
Users.userMetricsDF.get.groupBy("sd_year", "sd_month", "sd_day").agg(userColExpr, newUserColExpr, retUserColExpr).show()
*/

// using spark expresssion, get time-spent metrics
val expr =  org.apache.spark.sql.catalyst.analysis.UnresolvedFunction("round", Seq(
      org.apache.spark.sql.catalyst.expressions.Abs(
        org.apache.spark.sql.catalyst.expressions.Average(
          org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute("sd_time_spent")
        )
      ),
    Literal(2)))
Users.userMetricsDF.get.where(new Column(inAnalysisPeriodExpr).as("TimeSpent")).groupBy("sd_year", "sd_month", "sd_day").agg(new Column(expr)).show()
```
