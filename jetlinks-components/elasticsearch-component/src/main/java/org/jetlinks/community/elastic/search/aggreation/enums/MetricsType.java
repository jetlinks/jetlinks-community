package org.jetlinks.community.elastic.search.aggreation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.stats.Stats;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCount;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponse;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponseSingleValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public enum MetricsType {

    AVG("平均") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.avg(name).field(filed);
        }

        @Override
        public MetricsResponse getResponse(String name, SearchResponse response) {
            Avg avg = response.getAggregations().get(name);
            return MetricsResponse.builder()
                    .results(Collections.singletonMap(AVG,
                            new MetricsResponseSingleValue(avg.getValue(), avg.getValueAsString())))
                    .build();
        }
    },
    MAX("最大") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.max(name).field(filed);
        }

        @Override
        public MetricsResponse getResponse(String name, SearchResponse response) {
            Max max = response.getAggregations().get(name);
            return MetricsResponse.builder()
                    .results(Collections.singletonMap(MAX,
                            new MetricsResponseSingleValue(max.getValue(), max.getValueAsString())))
                    .build();
        }
    },
    VALUE_COUNT("非空值计数") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.count(name).field(filed);
        }

        @Override
        public MetricsResponse getResponse(String name, SearchResponse response) {
            ValueCount valueCount = response.getAggregations().get(name);
            return MetricsResponse.builder()
                    .results(Collections.singletonMap(VALUE_COUNT,
                            new MetricsResponseSingleValue(valueCount.getValue(), valueCount.getValueAsString())))
                    .build();
        }
    },
    MIN("最小") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.min(name).field(filed);
        }

        @Override
        public MetricsResponse getResponse(String name, SearchResponse response) {
            Min min = response.getAggregations().get(name);
            return MetricsResponse.builder()
                    .results(Collections.singletonMap(MIN,
                            new MetricsResponseSingleValue(min.getValue(), min.getValueAsString())))
                    .build();
        }
    },
    SUM("总数") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.sum(name).field(filed);
        }

        @Override
        public MetricsResponse getResponse(String name, SearchResponse response) {
            Sum sum = response.getAggregations().get(name);
            return MetricsResponse.builder()
                    .results(Collections.singletonMap(SUM,
                            new MetricsResponseSingleValue(sum.getValue(), sum.getValueAsString())))
                    .build();
        }
    },
    STATS("统计汇总") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.stats(name).field(filed);
        }

        @Override
        public MetricsResponse getResponse(String name, SearchResponse response) {
            Stats stats = response.getAggregations().get(name);
            Map<MetricsType, MetricsResponseSingleValue> results = new HashMap<>();
            results.put(AVG, new MetricsResponseSingleValue(stats.getAvg(), stats.getAvgAsString()));
            results.put(MIN, new MetricsResponseSingleValue(stats.getMin(), stats.getMinAsString()));
            results.put(MAX, new MetricsResponseSingleValue(stats.getMax(), stats.getMaxAsString()));
            results.put(SUM, new MetricsResponseSingleValue(stats.getSum(), stats.getMaxAsString()));
            results.put(VALUE_COUNT, new MetricsResponseSingleValue(stats.getCount(), String.valueOf(stats.getCount())));
            return MetricsResponse.builder()
                    .results(results)
                    .build();
        }
    };

    private String text;


    public abstract AggregationBuilder aggregationBuilder(String name, String filed);

    public abstract MetricsResponse getResponse(String name, SearchResponse response);

}
