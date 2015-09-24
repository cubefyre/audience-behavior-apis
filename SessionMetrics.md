# Session Metrics Analysis

Use this data product to peform:
 - trend analysis on sessions, revenue sessions and bounce session
    - apply filter to identify granular trends
    - segment trends by geo, host, campaign and more
    - segments and filters can be combined
 - Group by analysis for top-n over a period 
 - Get behavioral metrics on sessions
    - sessions with multiple events

> Trend Analysis

- Get daily, weekly, monthly trend on Sessions, Revenue Sessions, Bounce Sessions and sessions with other events 
- Add moving average to understand long-term trends and deviations from the norm

``` 
Example: Daily Trend Of Sessions Count And Bounce Sessions Count
Sessions.trendAny(sqlContext, 14, AggregateOnCol("sd_is_bounce_session", "Bounce Sessions", "countIf", 1), includeSessionTrend=true)
```

> Segmentation And Filtering

- Supply any dimension for segmentation and use the threshold value to limit the output records
- Find trend for a specific segment (for example - sessions when country is United States or browser is chrome)
- Supply segment name and segment value to filter trends
- To get filter values, run a SQL query

```
Example: Cart Sessions By Product Type
Sessions.trendAny(sqlContext, 7, AggregateOnCol("sd_is_cart_session", "Cart Sessions", "sum"), segmentBy=Some("sd_product_category"))

Example: Revenue Sessions Breakdown By Browser (Top 5)
Sessions.trend(sqlContext, 30, segmentBy=Some("sd_browser"), revenueSessionsTrendOnly=true, topNSegments=5)

```

> Grouping

- Over the last month, which cities sessions originated from?
- What browsers were uses for sessions?
- Top 10 hosts for new sessions
- Apply sorting and filtering

```
Example: Top 10 Cities For Revenue And Cart Sessions
Sessions.segment(sqlContext, 30, colList=Seq("sd_is_cart_session", "sd_is_revenue_session"), segmentBy=Some("sd_city"), topNSegments=10, includeDefaults=false)
```

> Extend Analysis Using SQL, Scala Or Python

```
--- Daily count of all sessions and sessions that captured all events - video, cart and revenue events.
select concat(sd_year, '-', sd_month, '-', sd_day) as date, count(sd_session_id) as all_sessions, sum(case when sd_is_revenue_session = 1 and sd_is_video_session = 1 and sd_is_cart_session = 1 then 1 else 0 end) as inclusive_sessions from sd_session_metrics where intervalContainsDateTime(intervalFromStr("2015-07-30T00:00:00.000Z/2015-08-07T00:00:00.000Z"),dateTime(`utc_time`)) group by sd_year, sd_month, sd_day
```

```
import org.apache.spark.sql.{Column, DataFrame}

// list only thoese rows that have column values greater than 10 - rev sessions > 100
val df = sql("select sd_year, sd_month, sd_day, sum(sd_is_revenue_session) as rev_sessions, sum(sd_is_cart_session) as cart_sessions from sd_session_metrics group by sd_year, sd_month, sd_day")
val sqlResults = df.where($"rev_sessions" > 100).map {
      //Implementing variable length argument to avoid a boat load of row case classes
      case org.apache.spark.sql.Row (sd_year: Int, sd_month: Int, sd_day: Int, vals@_*) => {
        sd_year + "-" + sd_month + "-" + sd_day + "\t" + vals.mkString ("\t ")
      }
    }.collect()

println (s"%table " + sqlResults.mkString ("\n") )
//Sessions.sessionMetricsDF.get.printSchema
```
