# Event Metrics Analysis

Use this data product to peform:
- trend analysis on page views and events
  - apply filter to identify granular trends, go deep in your data
  - segment trends by geo, host, campaign and more
  - segments and filters can be combined
- Group by analysis for top-n over a period 

> Trend Analysis

- Get daily, weekly, monthly trends for reveue generating events, all other events and page views
- Add moving average to understand long-term trends and deviations from the norm

``` 
Example: Daily Trend Of All Events Over Last 30 days
Events.trend(sqlContext, analysisPeriod=30, eventList=List("Completed Order", "Click Reviews", "AddToBag", "Click Slider", "Click menu", "Click Video"), includePageViewsTrend=false)
```

> Segmentation and Filtering

- Supply any dimension for segmentation and use the threshold value to limit the output records
- Find trend for a specific segment (for example - sessions when country is United States or browser is chrome)
- Supply segment name and segment value to filter trends
- To get filter values, run a SQL query

```
Example:  Weekly Trend of Events by Source
Events.trend(sqlContext, 4, periodType="weekly", segmentBy=Some("sd_source"), topNSegments=2)
```

> Group By

- Over the last month, which city had the most number of page views?
- Which browser ranked at the top for most 'Completed Order' events last week?
- Show me top 50 postal areas for last 2 weeks.
- Apply sorting and filtering

``` 
Example: Top 5 Cities With Add-To-Bag And Completed Order
Events.segment(sqlContext, 30, eventList=Seq("AddToBag", "Completed Order"), segmentBy=Some("sd_city"), sortBy=Seq("Completed Order"), topNSegments=5)
```

> Extend Analysis Using SQL, Scala Or Python

###### Flexibility to run Spark SQL and write Scala / Python code against a Spark cluster
- Cube is avaialble to you as dataframe in Scala and as a temporary table in the cluster for SQL access
- Run SQL queries directly against the curated data
- Write your own scala or python code and execute against the spark cluster

> Option For Apache Spark Development In Scala And Python Running Over The Same Spark Cluster

```
import org.apache.spark.sql.{Column, SQLContext, DataFrame, SaveMode}

Events.eventsDF.get.printSchema()

//simple count aggregation by day
Events.eventsDF.get.groupBy("sd_year", "sd_month", "sd_day").agg("page_name" -> "count").show()

//Count with custom names
Events.eventsDF.get.groupBy("sd_year", "sd_month", "sd_day").agg(org.apache.spark.sql.functions.count("page_name").as("Page Views"), org.apache.spark.sql.functions.count("event_name").as("Events")).show()

// Case Statement to remove nulls: CASE WHEN THEN ELSE END
Events.eventsDF.get.groupBy("sd_year", "sd_month", "sd_day").agg(org.apache.spark.sql.functions.count(new Column("page_name")).as("Page Views"), org.apache.spark.sql.functions.sum(when(Events.eventsDF.get("event_name").isNotNull === true, 1).otherwise(0)).as("Events")).show()
//Events.eventsDF.get.filter(new Column("event_name").isNotNull).groupBy("sd_year", "sd_month", "sd_day", "event_name").count.orderBy("sd_year", "sd_month", "sd_day", "count").show()

// Count of events and events of type AddToBag
Events.eventsDF.get.groupBy("sd_year", "sd_month", "sd_day").agg(org.apache.spark.sql.functions.sum(when(Events.eventsDF.get("event_name").isNotNull === true, 1).otherwise(0)).as("Events"), org.apache.spark.sql.functions.sum(when(Events.eventsDF.get("event_name").equalTo("AddToBag"), 1).otherwise(0)).as("Events")).show()
```
