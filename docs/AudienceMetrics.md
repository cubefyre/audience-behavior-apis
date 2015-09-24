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
