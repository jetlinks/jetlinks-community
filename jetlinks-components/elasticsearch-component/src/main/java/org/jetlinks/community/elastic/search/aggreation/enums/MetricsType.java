package org.jetlinks.community.elastic.search.aggreation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.*;
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
                    new MetricsResponseSingleValue(avg.getValue(), avg.getName(), avg.getValueAsString())))
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
                    new MetricsResponseSingleValue(max.getValue(), max.getName(), max.getValueAsString())))
                .build();
        }
    },
    COUNT("非空值计数") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.count(name).field(filed);
        }

        @Override
        public MetricsResponse getResponse(String name, SearchResponse response) {
            ValueCount valueCount = response.getAggregations().get(name);
            return MetricsResponse.builder()
                .results(Collections.singletonMap(COUNT,
                    new MetricsResponseSingleValue(valueCount.getValue(), valueCount.getName(), valueCount.getValueAsString())))
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
                    new MetricsResponseSingleValue(min.getValue(), min.getName(), min.getValueAsString())))
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
                    new MetricsResponseSingleValue(sum.getValue(), sum.getName(), sum.getValueAsString())))
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
            results.put(AVG, new MetricsResponseSingleValue(stats.getAvg(), stats.getName(), stats.getAvgAsString()));
            results.put(MIN, new MetricsResponseSingleValue(stats.getMin(), stats.getName(), stats.getMinAsString()));
            results.put(MAX, new MetricsResponseSingleValue(stats.getMax(), stats.getName(), stats.getMaxAsString()));
            results.put(SUM, new MetricsResponseSingleValue(stats.getSum(), stats.getName(), stats.getMaxAsString()));
            results.put(COUNT, new MetricsResponseSingleValue(stats.getCount(), stats.getName(), String.valueOf(stats.getCount())));
            return MetricsResponse.builder()
                .results(results)
                .build();
        }
    };

    private String text;


    public abstract AggregationBuilder aggregationBuilder(String name, String filed);

    public abstract MetricsResponse getResponse(String name, SearchResponse response);

    public static MetricsType of(String name) {
        for (MetricsType type : MetricsType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new UnsupportedOperationException("不支持的聚合度量类型：" + name);
    }

}
