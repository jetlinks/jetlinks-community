package org.jetlinks.community.elastic.search.aggreation.enums;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.range.DateRangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.range.RangeAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.jetlinks.community.elastic.search.aggreation.bucket.AggregationResponseHandle;
import org.jetlinks.community.elastic.search.aggreation.bucket.Bucket;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketAggregationsStructure;
import org.jetlinks.community.elastic.search.aggreation.bucket.Sort;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public enum BucketType {


    TERMS("字段项") {
        @Override
        public AggregationBuilder aggregationBuilder(BucketAggregationsStructure structure) {
            TermsAggregationBuilder builder = AggregationBuilders
                .terms(structure.getName())
                .field(structure.getField());
            if (structure.getSize() != null) {
                builder.size(structure.getSize());
            }
            Sort sort = structure.getSort();
            if (sort != null) {
                builder.order(mapping.get(OrderBuilder.of(sort.getOrder(), sort.getType())));
            }
            if (structure.getMissingValue() != null) {
                builder.missing(structure.getMissingValue());
            }
            commonAggregationSetting(builder, structure);
            return builder;
        }

        @Override
        public <A extends Aggregation> List<Bucket> convert(A a) {
            return AggregationResponseHandle.terms(a);
        }
    },
    RANGE("范围") {
        @Override
        public AggregationBuilder aggregationBuilder(BucketAggregationsStructure structure) {
            RangeAggregationBuilder builder = AggregationBuilders
                .range(structure.getName())
                .field(structure.getField());
            if (StringUtils.hasText(structure.getFormat())) {
                String format = structure.getFormat();
                if (format.startsWith("yyyy")) {
                    format = "8" + format;
                }
                builder.format(format);
            }
            structure.getRanges()
                .forEach(ranges -> {
                    builder.addRange(ranges.getKey(), (Double) ranges.getForm(), (Double) ranges.getTo());
                });
            commonAggregationSetting(builder, structure);
            return builder;
        }

        @Override
        public <A extends Aggregation> List<Bucket> convert(A a) {
            return AggregationResponseHandle.range(a);
        }
    },
    DATE_RANGE("时间范围") {
        @Override
        public AggregationBuilder aggregationBuilder(BucketAggregationsStructure structure) {
            DateRangeAggregationBuilder builder = AggregationBuilders
                .dateRange(structure.getName())
                .field(structure.getField());
            if (StringUtils.hasText(structure.getFormat())) {
                String format = structure.getFormat();
                if (format.startsWith("yyyy")) {
                    format = "8" + format;
                }
                builder.format(format);
            }
            structure.getRanges()
                .forEach(ranges -> {
                    builder.addRange(ranges.getKey(), ranges.getForm().toString(), ranges.getTo().toString());
                });
            if (structure.getMissingValue() != null) {
                builder.missing(structure.getMissingValue());
            }
            builder.timeZone(ZoneId.systemDefault());
            commonAggregationSetting(builder, structure);
            return builder;
        }

        @Override
        public <A extends Aggregation> List<Bucket> convert(A a) {
            return AggregationResponseHandle.range(a);
        }
    },
    DATE_HISTOGRAM("时间范围") {
        @Override
        public AggregationBuilder aggregationBuilder(BucketAggregationsStructure structure) {
            DateHistogramAggregationBuilder builder = AggregationBuilders
                .dateHistogram(structure.getName())
                .field(structure.getField());
            if (StringUtils.hasText(structure.getFormat())) {
                builder.format(structure.getFormat());
            }
            if (StringUtils.hasText(structure.getInterval())) {
                builder.dateHistogramInterval(new DateHistogramInterval(structure.getInterval()));
            }
            if (structure.getExtendedBounds() != null) {
                builder.extendedBounds(structure.getExtendedBounds());
            }
            if (structure.getMissingValue() != null) {
                builder.missing(structure.getMissingValue());
            }
            Sort sort = structure.getSort();
            if (sort != null) {
                builder.order(mapping.get(OrderBuilder.of(sort.getOrder(), sort.getType())));
            }
            builder.timeZone(ZoneId.systemDefault());
            commonAggregationSetting(builder, structure);
            return builder;
        }


        @Override
        public <A extends Aggregation> List<Bucket> convert(A a) {
            return AggregationResponseHandle.dateHistogram(a);
        }
    };

    private final String text;

    public abstract AggregationBuilder aggregationBuilder(BucketAggregationsStructure structure);

    public abstract <A extends Aggregation> List<Bucket> convert(A a);

    private static void commonAggregationSetting(AggregationBuilder builder, BucketAggregationsStructure structure) {
        if (structure.getSubMetricsAggregation() != null && structure.getSubMetricsAggregation().size() > 0) {
            addMetricsSubAggregation(builder, structure.getSubMetricsAggregation());
        }
        if (structure.getSubBucketAggregation() != null && structure.getSubBucketAggregation().size() > 0) {
            addBucketSubAggregation(builder, structure.getSubBucketAggregation());
        }
    }

    private static void addMetricsSubAggregation(AggregationBuilder builder, List<MetricsAggregationStructure> subMetricsAggregation) {
        subMetricsAggregation
            .forEach(subStructure -> {
                builder.subAggregation(subStructure.getType().aggregationBuilder(subStructure.getName(), subStructure.getField()));
            });
    }

    private static void addBucketSubAggregation(AggregationBuilder builder, List<BucketAggregationsStructure> subBucketAggregation) {
        subBucketAggregation
            .forEach(subStructure -> {
                builder.subAggregation(subStructure.getType().aggregationBuilder(subStructure));
            });
    }

    @Getter
    @AllArgsConstructor(staticName = "of")
    @EqualsAndHashCode
    static class OrderBuilder {

        private String order;

        private OrderType orderType;
    }

    static Map<OrderBuilder, BucketOrder> mapping = new HashMap<>();

    static {
        mapping.put(OrderBuilder.of("asc", OrderType.COUNT), BucketOrder.count(true));
        mapping.put(OrderBuilder.of("desc", OrderType.COUNT), BucketOrder.count(false));
        mapping.put(OrderBuilder.of("asc", OrderType.KEY), BucketOrder.key(true));
        mapping.put(OrderBuilder.of("desc", OrderType.KEY), BucketOrder.key(false));
    }

}
