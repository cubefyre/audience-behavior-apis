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

> 
