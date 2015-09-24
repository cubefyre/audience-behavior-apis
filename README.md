# audience-behavior-server

Scala based API solution to build audience behavior solution on Spark Cluster

API Guide
---

***Event Metrics***
---
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
