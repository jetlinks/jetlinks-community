package org.jetlinks.community.elastic.search.service.reactive;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

@AllArgsConstructor
public enum AggType {

    AVG("平均") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.avg(name).field(filed).missing(0);
        }

    },
    MAX("最大") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.max(name).field(filed).missing(0);
        }
    },
    MEDIAN("中间值") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.medianAbsoluteDeviation(name).field(filed).missing(0);
        }
    },
    STDDEV("标准差") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.extendedStats(name)
                                      .field(filed)
                                      .missing(0);
        }
    },
    COUNT("非空值计数") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.count(name).field(filed).missing(0);
        }

    },
    DISTINCT_COUNT("去重计数") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders
                .cardinality(name)
                .field(filed)
                .missing(0);
        }

    },
    MIN("最小") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.min(name).field(filed).missing(0);
        }
    },
    FIRST("第一条数据") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.topHits(name).size(1);
        }
    },
    TOP("第N条数据") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.topHits(name);
        }
    },
    SUM("总数") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.sum(name).field(filed).missing(0);
        }
    },
    STATS("统计汇总") {
        @Override
        public AggregationBuilder aggregationBuilder(String name, String filed) {
            return AggregationBuilders.stats(name).field(filed).missing(0);
        }

    };

    @Getter
    private final String text;

    public abstract AggregationBuilder aggregationBuilder(String name, String filed);

    public static AggType of(String name) {
        for (AggType type : AggType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new UnsupportedOperationException("不支持的聚合类型：" + name);
    }

}
