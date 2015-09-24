# Conversion Analysis

Use this data product to perform:
 - conversion rate analysis on users
    - apply filter to identify granular trends, go deep in your data
    - segment trends by geo, host, campaign and more
    - segments and filters can be combined
 - Group by analysis for top-n over a period 
  
> Conversion Rate Trend Analysis

- Get daily, weekly, monthly trends for conversion
- Add moving average to understand long-term trends and deviations from the norm
- Conversion is defined as ratio of revenue users to all users

``` 
Example: Over The Last 30 Days With 10 Day Moving Average
Conversions.trend(sqlContext, 30, includeDefaults=false, movingAverage=Some(10))
```


> Conversion Rate by Segment and Filtering

- Supply any dimension for segmentation and use the threshold value to limit the output records
- Find trend for a specific segment (for example - rates when country is United States or browser is chrome)
- Supply segment name and segment value to filter conversion rates
- To get filter values, run a SQL query
- Filtering on aggregated Columns

```
Example: Conversion Rate When More Than 50 Users Converted
Conversions.trend(sqlContext, 60, aggFilterOn=Some("Converted"), aggFilterOp="gt", aggFilterVal=Some(50))

Example: Conversion Rates From Facebook Campaign
Conversions.trend(sqlContext, 15, includeDefaults=false, filterOn=Some("sd_campaign"), filterVal=Some("d-all-retargeting-facebook"))
```

> Top N (Grouping)

- Over the last month, which city had the highest conversion rate?
- Which browser ranked at the top last week?
- Show me top 50 postal areas for last 2 weeks.
- Apply sorting and filtering

``` 
Example: Conversion Rate By Cities (Min 1000 Users)
Conversions.segment(sqlContext, 60, segmentBy=Some("sd_city"), aggFilterOn=Some("Total"), aggFilterOp="gt", aggFilterVal=Some(1000))
```

> Extend Analysis Using SQL, Scala Or Python

Flexibility to run Spark SQL and write Scala / Python code against a Spark cluster
- Cube is avaialble to you as dataframe in Scala and as a temporary table in the cluster for SQL access
- Run SQL queries directly against the curated data
- Write your own scala or python code and execute against the spark cluster

```
Run Spark And HIVE SQL
--desc sd_conversion_rate

-- Segment conversion rates by two dimensions
select sd_year, sd_month, sd_day, ${1st-Dimension=sd_city, sd_campaign|sd_source|sd_medium|sd_content|sd_term|sd_browser|sd_device_os|sd_device_family|sd_country_code|sd_country_name|sd_region|sd_city}, ${2nd-Dimension=sd_browser, sd_campaign|sd_source|sd_medium|sd_content|sd_term|sd_browser|sd_device_os|sd_device_family|sd_country_code|sd_country_name|sd_region|sd_city},
sum(sd_rev_user) RevenueUsers, sum(sd_all_user) Users,  round( (sum(sd_rev_user)/ sum(sd_all_user))*100, 2) ConversionRate
from sd_conversion_rate
group by sd_year, sd_month, sd_day, ${1st-Dimension=sd_city, sd_campaign|sd_source|sd_medium|sd_content|sd_term|sd_browser|sd_device_os|sd_device_family|sd_country_code|sd_country_name|sd_region|sd_city},
 ${2nd-Dimension=sd_browser, sd_campaign|sd_source|sd_medium|sd_content|sd_term|sd_browser|sd_device_os|sd_device_family|sd_country_code|sd_country_name|sd_region|sd_city}
having sum(sd_rev_user) > ${Minimum Paying Users=0}
```
![Image](img/CRA.png?raw=true)

```
Scala

import org.apache.spark.sql.{Column, DataFrame}

Conversions.convRateDF.get.printSchema()

//simple count aggregation by day
Conversions.convRateDF.get.groupBy("sd_year", "sd_month", "sd_day").agg("sd_all_user" -> "count").show()

//Conversion Rate
val revUsersColExpr =  org.apache.spark.sql.functions.sum(new Column("sd_rev_user"))
val allUsersColExpr =  org.apache.spark.sql.functions.sum(new Column("sd_all_user"))
val converstionRateColExpr = revUsersColExpr.divide(allUsersColExpr).multiply(100).alias("Conversion Rate")
Conversions.convRateDF.get.groupBy("sd_year", "sd_month", "sd_day").agg(converstionRateColExpr).show()
```
