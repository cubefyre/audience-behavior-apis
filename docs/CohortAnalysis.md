# Cohort Analysis

Daily Conversion Rates For Cohort - use this data product to peform:
 - conversion analysis by cohorts
    - apply filter to identify granular trends, go deep in your data
    - apply filters on aggregated columns

> Conversion Rate By Daily Cohorts

```
Users.cohort(sqlContext, startDaysAgo=7, daysToConversionLimit=5, aggFilterOn=Some("ConversionRate"),aggFilterOp="gt", aggFilterVal=Some(0))
```
![Image](img/CA.png?raw=true)

###### How to intrepret the graph?
- Conversion Rate by Daily Cohorts maps the rate at which returning users “Convert” N days after they first visit the site.
- The Y axis is the date of the Cohort. The date represents the set of users who FIRST visited the site on that day.
- The X axis is the number of days after the COHORT DATE( as shown in the Y axis) that the converted to a paid user.
- The size of the circle represents the conversion rate or the number of users in that cohort that converted as a ratio of the number of users in that cohort who visited.
The value of this chart is that you can instantly see the pattern of conversion for returning users.
- Mapping these patterns repeatedly helps understand the purchase behavior of your customers.

> Run Spark And HIVE SQL

```
-- cohort query
select sd_year, sd_month, sd_day, (dayOfYear(dateTime(`utc_time`)) - dayOfYear(dateTimeFromEpoch(`sd_first_seen_at`))) as days_to_conversion,
sum(sd_is_revenue_user) as revenue_users, count(user_id) as all_users
from sd_user_metricsII where dateIsAfterOrEqual(dateTimeFromEpoch(`sd_first_seen_at`),dateTimeFromEpoch(1438905600000)) and intervalContainsDateTime(interval(dateTimeFromEpoch(`sd_first_seen_at`),datePlus(dateTimeFromEpoch(`sd_first_seen_at`),period("P7D"))),dateTime(`utc_time`))
group by sd_year, sd_month, sd_day, (dayOfYear(dateTime(`utc_time`)) - dayOfYear(dateTimeFromEpoch(`sd_first_seen_at`)))
```