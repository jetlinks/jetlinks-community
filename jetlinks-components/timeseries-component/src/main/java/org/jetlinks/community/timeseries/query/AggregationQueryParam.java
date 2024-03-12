package org.jetlinks.community.timeseries.query;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.dsl.Query;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.Interval;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 聚合查询参数
 */
@Getter
@Setter
public class AggregationQueryParam {

    //聚合列
    private List<AggregationColumn> aggColumns = new ArrayList<>();

    //按时间分组
    @Deprecated
    private TimeGroup groupByTime;

    //按字段分组
    private List<Group> groupBy = new ArrayList<>();

    //最大返回记录条数
    private int limit;

    private long startWithTime = 0;

    private long endWithTime = DateTime.now()
                                       .withHourOfDay(23)
                                       .withMinuteOfHour(59)
                                       .withSecondOfMinute(59)
                                       .withMillisOfSecond(0)
                                       .getMillis();

    private String timeProperty = "timestamp";

    //条件过滤
    private QueryParamEntity queryParam = new QueryParamEntity();

    public static AggregationQueryParam of() {
        return new AggregationQueryParam();
    }

    public <T> T as(Function<AggregationQueryParam, T> mapper) {
        return mapper.apply(this);
    }

    public AggregationQueryParam from(long time) {
        this.startWithTime = time;
        return this;
    }

    public AggregationQueryParam from(Date time) {
        if (null != time) {
            return from(time.getTime());
        }
        return this;
    }

    public AggregationQueryParam to(long time) {
        this.endWithTime = time;
        return this;
    }

    public AggregationQueryParam to(Date time) {
        if (null != time) {
            return to(time.getTime());
        }
        return this;
    }

    public AggregationQueryParam agg(AggregationColumn agg) {
        aggColumns.add(agg);
        return this;
    }

    public AggregationQueryParam agg(String property, String alias, Aggregation agg) {
        return this.agg(new AggregationColumn(property, alias, agg));
    }

    public AggregationQueryParam agg(String property, String alias, Aggregation agg, Object defaultValue) {
        return this.agg(new AggregationColumn(property, alias, agg, defaultValue));
    }


    public AggregationQueryParam agg(String property, Aggregation agg) {
        return agg(property, property, agg);
    }

    public AggregationQueryParam sum(String property, String alias) {
        return agg(property, alias, Aggregation.SUM);
    }

    public AggregationQueryParam sum(String property) {
        return agg(property, Aggregation.SUM);
    }

    public AggregationQueryParam avg(String property, String alias) {
        return agg(property, alias, Aggregation.AVG);
    }

    public AggregationQueryParam avg(String property) {
        return agg(property, Aggregation.AVG);
    }

    public AggregationQueryParam count(String property, String alias) {
        return agg(property, alias, Aggregation.COUNT);
    }

    public AggregationQueryParam count(String property) {
        return agg(property, Aggregation.COUNT);
    }

    public AggregationQueryParam max(String property, String alias) {
        return agg(property, alias, Aggregation.MAX);
    }

    public AggregationQueryParam max(String property) {
        return agg(property, Aggregation.MAX);
    }

    public AggregationQueryParam min(String property, String alias) {
        return agg(property, alias, Aggregation.MIN);
    }

    public AggregationQueryParam min(String property) {
        return agg(property, Aggregation.MIN);
    }

    public AggregationQueryParam groupBy(Interval time, String alias, String format) {
        if (null == time) {
            return this;
        }
        return groupBy(new TimeGroup(time, alias, format));
    }

    public AggregationQueryParam groupBy(Interval time, String format) {
        return groupBy(time, "time", format);
    }

    public AggregationQueryParam groupBy(TimeGroup timeGroup) {
        this.groupByTime = timeGroup;
        return this;
    }

    public AggregationQueryParam groupBy(Group group) {
        groupBy.add(group);
        return this;
    }

    public AggregationQueryParam groupBy(String property, String alias) {
        return groupBy(new Group(property, alias));
    }

    public AggregationQueryParam groupBy(String property) {
        return groupBy(new Group(property, property));
    }

    public <T> T execute(Function<AggregationQueryParam, T> executor) {
        return executor.apply(this);
    }

    public AggregationQueryParam filter(Consumer<Query<?, QueryParam>> consumer) {
        consumer.accept(Query.of(queryParam));
        return this;
    }

    public AggregationQueryParam filter(QueryParamEntity queryParam) {
        this.queryParam = queryParam;
        return this;
    }

    public AggregationQueryParam limit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<Group> getGroups() {
        if (getGroupByTime() == null) {
            return groupBy;
        }
        List<Group> groups = new ArrayList<>();
        groups.add(getGroupByTime());
        groups.addAll(groupBy);
        return groups;
    }

}
