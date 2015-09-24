# audience-behavior-server

Scala based API solution to build audience behavior solution on Spark Cluster

API Guide
---

***Event Metrics***
---
Use these APIs to:
- execute trend with a set of parameters to understand trend
- execute segment to group events information over a period

> Trend Analysis for Events and Page Views

```
trend(
  sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  eventList : Seq[String] = Seq(),
  includePageViewsTrend : Boolean = true,
  includeEventsTrend : Boolean = true,
  periodType : String = "daily",
  movingAverage : Option[Int] = None,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  weekendOnly : Boolean = false
) 

trendAny(
  sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  aggregateOnCol: AggregateOnCol = eventAggCol,
  periodType : String = "daily",
  movingAverage : Option[Int] = None,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  weekendOnly : Boolean = false
)
```
---
> TopN - GroupBy Over a Period 

```
//segment multiple cols over a period
segment(
  sqlContext: SQLContext,
  analysisPeriod: Int = 7,
  eventList : Seq[String] = Seq(),
  includeDefaults : Boolean = true,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  sortBy : Seq[String] = Seq()
)

//segment single col over a period
segmentAny(
  sqlContext: SQLContext,
  aggregateOnCol: AggregateOnCol = eventAggCol,
  analysisPeriod: Int = 7,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  sortBy : Seq[String] = Seq()
) 
```
---
***Session Metrics***
---
Use these APIs to:
- execute trend with a set of parameters to understand trend of sessions
- execute segment to group session information over a period
- execute behavior to understand what sessions showed key behavior

> Trend Analysis for Sessions

```
trend(
  sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  periodType : String = "daily",
  sessionsTrendOnly : Boolean = false,
  revenueSessionsTrendOnly : Boolean = false,
  movingAverage : Option[Int] = None,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  weekendOnly : Boolean = false
)

trendAny(
  sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  aggregateOnCol: AggregateOnCol = sessionAggCol,
  periodType : String = "daily",
  includeSessionTrend : Boolean = false,
  includeRevenueSessionTrend : Boolean = false,
  movingAverage : Option[Int] = None,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  weekendOnly : Boolean = false
)
```
---
> Segments

```
//segment values over a period
segment(sqlContext: SQLContext,
  analysisPeriod: Int = 7,
  colList : Seq[String] = Seq(cartSessionColName, videoSessionColName),
  includeDefaults : Boolean = true,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  sortBy : Seq[String] = Seq(),
  verbose: Boolean = false
) 

// segment any values over a period
def segmentAny(sqlContext: SQLContext,
  analysisPeriod: Int = 7,
  aggregateOnCol: AggregateOnCol = revSessionAggCol,
  includeDefaults : Boolean = true,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  sortBy : Seq[String] = Seq(),
)
```
---
> Session Behavior Analysis

``` 
// What sessions are video sessions, add-to-cart sessions, revenue sessions - exclusive behavior
behavior(sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  periodType : String = "daily",
  revenueSessionsOnly : Boolean = false,
  videoSessionOnly : Boolean = false,
  cartSessionsOnly : Boolean = false,
  bounceSessionsOnly : Boolean = false,
  movingAverage : Option[Int] = None,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  weekendOnly : Boolean = false
)

// Inclusive behavior analysis - sessions in which users did collection of events
behaviorInclusive(sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  colList : Seq[String] = Seq(revSessionColName, cartSessionColName, videoSessionColName),
  periodType : String = "daily",
  movingAverage : Option[Int] = None,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  weekendOnly : Boolean = false
)
```
---

***User Meterics***
---
Use these APIs to:
- execute trend with a set of parameters to understand trend of users
- execute segment to group users information over a period
- execute behavior to understand what users showed key behavior

> Trend Analysis for Users

```
trend(
  sqlContext : SQLContext,
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
  weekendOnly : Boolean = false
)

trendAny(
  sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  aggregateOnCol: AggregateOnCol = userAggCol,
  periodType : String = "daily",
  movingAverage : Option[Int] = None,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  weekendOnly : Boolean = false
)
```
---
> User Segments

```
//segment values over a period
segment(
  sqlContext: SQLContext,
  analysisPeriod: Int = 7,
  colList : Seq[String] = Seq(newUserColName, revUserColName),
  includeDefaults : Boolean = true,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  sortBy : Seq[String] = Seq()
)

//segment any values over a period
segmentAny(
  sqlContext: SQLContext,
  analysisPeriod: Int = 7,
  aggregateOnCol: AggregateOnCol = revUserAggCol,
  includeDefaults : Boolean = true,
  segmentBy: Option[String] = None,
  topNSegments : Int = 10,
  filterOn: Option[String] = None,
  filterOp: String = "eq",
  filterVal: Option[Any] = None,
  sortBy : Seq[String] = Seq()
)

```
---
> User Engagement Analysis

```
// Engagement metrics - average session count, time spent and events fired
// ranked using n-tiles on session count, time spent and events fired
engagement(
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
  weekendOnly : Boolean = false
)
```
---
> User Behavior Analysis

``` 
// behavior analysis
// what did users do - video sessions, add-to-cart sessions, revenue sessions - exclusive behavior
behavior(
  sqlContext : SQLContext,
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
  weekendOnly : Boolean = false
)


//inclusive behavior analysis - users did collection of events
behaviorInclusive(
  sqlContext : SQLContext,
  analysisPeriod : Int = 7,
  forRevenueUsers : Boolean = false,
  colList : Seq[String] = Seq(revSessionsColName, cartSessionsColName,     videoSessionsColName),
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
  weekendOnly : Boolean = false
)
```
---

*** Cohort Analysis***
---
> Cohort Analysis for Users

```
cohort(
    sqlContext : SQLContext,
    startDaysAgo : Int = 14,
    daysToConvertLimit : Int = 7,
    filterOn: Option[String] = None,
    filterOp: String = "eq",
    filterVal: Option[Any] = None,
    aggFilterOn : Option[String] = None,
    aggFilterOp: String = "eq",
    aggFilterVal: Option[Any] = None,
    weekendOnly : Boolean = false
) 
---
> Definition of Input Parameters:

- Use the startDaysAgo to specify the analysis window. Default value is 14 days. 
- Use daysToConvertLimit to change the...
- Use filterOn, filterOp and filterVal parameters to filter analysis. The default value for filter operator (filterOp) is eq.  
    - Other values this parameter can take are geq, leq, lt, gt, like.
---
> How to intrepret the graph

- Conversion Rate by Daily Cohorts maps the rate at which returning users “Convert” N days after they first visit the site.

- The Y axis is the date of the Cohort. The date represents the set of users who FIRST visited the site on that day.

- The X axis is the number of days after the COHORT DATE( as shown in the Y axis) that the converted to a paid user.

- The size of the circle represents the conversion rate or the number of users in that cohort that converted as a ratio of the number of users in that cohort who visited.
The value of this chart is that you can instantly see the pattern of conversion for returning users.

- Mapping these patterns repeatedly helps understand the purchase behavior of your customers.


---


> Definition of Input Parameters:

- Use the analysis period to specify the analysis window. Default value is 7 days. 
- Use periodType to change the aggregate period from daily to weekly or monthly. Default value is daily.
- Use movingAverage to include moving averages for each metrics. Moving averages are not included by default. The integer value for  movingAverage will determine the period for moving average. Few examples are: 
    - For 5 day moving average (DMA), periodType is daily and value for movingAverage is 5. 
    - For 4 week moving average (WMA), periodType is weekly and value for movingAverage is 4. 
- Use segmentBy to segment analysis by a dimension. See below for a list of available dimensions for this cube.
- Use topNSegments to limit number of segments in any analysis output.
- Use filterOn, filterOp and filterVal parameters to filter analysis. The default value for filter operator (filterOp) is eq.  
    - Other values this parameter can take are geq, leq, lt, gt, like.
- Use weekendOnly filter to filter analysis for weekends only.
- Use sortBy to order results using a dimension in ascending or descending order.

---

> Dimensions available for filtering and segmenting:

    - campaign_name
    - campaign_content
    - campaign_medium
    - campaign_source
    - campaign_term
    - sd_host
    - sd_path
    - sd_campaign
    - sd_source
    - sd_medium
    - sd_content
    - sd_term
    - sd_browser
    - sd_device_os
    - sd_device_family
    - sd_country_code
    - sd_country_name
    - sd_region
    - sd_city
    - sd_postal_code
    - sd_continent
    
*Tip: Run a SQL command to get values for a dimension to use in the filter*

---

*Note: Functions trendAny and segmentAny are for advanced users. Use these if none of the pre-baked metrics in the trend or segment functions above fit your requirements. You can also change the aggregate function to count, countd, sum and more.*
