package org.jetlinks.community.elastic.search.service.reactive;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.utils.QueryParamTranslator;

import java.util.Collections;

@Getter
@AllArgsConstructor
public enum AggType {

    AVG("平均") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {
            return builder.avg(avg -> avg
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    MAX("最大") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {
            return builder.max(max -> max
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    MEDIAN("中间值") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {
            return builder.medianAbsoluteDeviation(m -> m
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    STDDEV("标准差") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {
            return builder.extendedStats(avg -> avg
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    COUNT("非空值计数") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {

            return builder.filter(q -> QueryParamTranslator
                .applyQueryBuilder(q,
                                   Collections.singletonList(Term.of(field, TermType.notnull, field)),
                                   metadata));
        }
    },
    DISTINCT_COUNT("去重计数") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {

            return builder.cardinality(cardinality -> cardinality
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    MIN("最小") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {

            return builder.min(cardinality -> cardinality
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    FIRST("第一条数据") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {

            return builder.topHits(top -> top
                .size(1)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    TOP("第N条数据") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name,
                                                                       String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {

            return builder.topHits(top -> top
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    SUM("总和") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {

            return builder.sum(b -> b
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }
    },
    STATS("统计汇总") {
        @Override
        public Aggregation.Builder.ContainerBuilder aggregationBuilder(String name, String field,
                                                                       ElasticSearchIndexMetadata metadata,
                                                                       Aggregation.Builder builder,
                                                                       Object missing) {

            return builder.stats(b -> b
                .field(field)
                .missing(missing == null ? null : FieldValue.of(JsonData.of(missing))));
        }

    };

    @Getter
    private final String text;

    public abstract ObjectBuilder<Aggregation> aggregationBuilder(String name,
                                                                  String field,
                                                                  ElasticSearchIndexMetadata metadata,
                                                                  Aggregation.Builder builder,
                                                                  Object missing);


    public static AggType of(String name) {
        for (AggType type : AggType.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new UnsupportedOperationException("不支持的聚合类型：" + name);
    }

}