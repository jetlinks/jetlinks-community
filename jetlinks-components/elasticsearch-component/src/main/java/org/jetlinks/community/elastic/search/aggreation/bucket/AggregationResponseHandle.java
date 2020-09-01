package org.jetlinks.community.elastic.search.aggreation.bucket;

import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.range.Range;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.*;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponseSingleValue;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public class AggregationResponseHandle {


    public static <A extends Aggregation> List<Bucket> terms(A a) {
        Terms terms = (Terms) a;
        return terms.getBuckets()
            .stream()
            .map(b -> {
                Bucket bucket = Bucket.builder()
                    .key(b.getKeyAsString())
                    .count(b.getDocCount())
                    .name(a.getName())
                    .build();
                b.getAggregations().asList()
                    .forEach(subAggregation -> route(bucket, subAggregation));
                return bucket;
            }).collect(Collectors.toList())
            ;
    }

    public static <A extends Aggregation> List<Bucket> range(A a) {
        Range range = (Range) a;
        return range.getBuckets()
            .stream()
            .map(b -> {
                Bucket bucket = Bucket.builder()
                    .key(b.getKeyAsString())
                    .from(b.getFrom())
                    .to(b.getTo())
                    .fromAsString(b.getFromAsString())
                    .toAsString(b.getToAsString())
                    .count(b.getDocCount()).build();
                b.getAggregations().asList()
                    .forEach(subAggregation -> {
                        route(bucket, subAggregation);
                    });
                return bucket;
            }).collect(Collectors.toList())
            ;
    }

    public static <A extends Aggregation> List<Bucket> dateHistogram(A a) {
        Histogram histogram = (Histogram) a;
        return bucketsHandle(histogram.getBuckets(), a.getName());
    }

    private static List<Bucket> bucketsHandle(List<? extends Histogram.Bucket> buckets, String name) {
        return buckets
            .stream()
            .map(b -> {
                Bucket bucket = Bucket.builder()
                    .key(b.getKeyAsString())
                    .count(b.getDocCount())
                    .name(name)
                    .build();
                b.getAggregations().asList()
                    .forEach(subAggregation -> route(bucket, subAggregation));
                return bucket;
            }).collect(Collectors.toList())
            ;
    }

    private static <A extends Aggregation> void route(Bucket bucket, A a) {
        if (a instanceof Terms) {
            bucket.setBuckets(terms(a));
        } else if (a instanceof Range) {
            bucket.setBuckets(range(a));
        } else if (a instanceof Histogram) {
            bucket.setBuckets(range(a));
        } else if (a instanceof Avg) {
            bucket.setAvg(avg(a));
        } else if (a instanceof Min) {
            bucket.setMin(min(a));
        } else if (a instanceof Max) {
            bucket.setMax(max(a));
        } else if (a instanceof Sum) {
            bucket.setSum(sum(a));
        } else if (a instanceof Stats) {
            stats(bucket, a);
        }  else if (a instanceof ValueCount) {
            bucket.setValueCount(count(a));
        } else {
            throw new UnsupportedOperationException("不支持的聚合类型");
        }
    }

    public static <A extends Aggregation> MetricsResponseSingleValue count(A a) {
        ValueCount max = (ValueCount) a;
        return MetricsResponseSingleValue.builder()
            .value(max.getValue())
            .name(a.getName())
            .valueAsString(max.getValueAsString())
            .build();
    }

    public static <A extends Aggregation> MetricsResponseSingleValue avg(A a) {
        Avg avg = (Avg) a;
        return MetricsResponseSingleValue.builder()
            .value(avg.getValue())
            .name(a.getName())
            .valueAsString(avg.getValueAsString())
            .build();
    }

    public static <A extends Aggregation> MetricsResponseSingleValue max(A a) {
        Max max = (Max) a;
        return MetricsResponseSingleValue.builder()
            .value(max.getValue())
            .name(a.getName())
            .valueAsString(max.getValueAsString())
            .build();
    }

    public static <A extends Aggregation> MetricsResponseSingleValue min(A a) {
        Min min = (Min) a;
        return MetricsResponseSingleValue.builder()
            .value(min.getValue())
            .name(a.getName())
            .valueAsString(min.getValueAsString())
            .build();
    }

    public static <A extends Aggregation> MetricsResponseSingleValue sum(A a) {
        Sum sum = (Sum) a;
        return MetricsResponseSingleValue.builder()
            .value(sum.getValue())
            .name(a.getName())
            .valueAsString(sum.getValueAsString())
            .build();
    }

    public static <A extends Aggregation> void stats(Bucket bucket, A a) {
        Stats stats = (Stats) a;
        bucket.setAvg(MetricsResponseSingleValue.builder()
            .value(stats.getAvg())
            .name(a.getName())
            .valueAsString(stats.getAvgAsString())
            .build());
        bucket.setMax(MetricsResponseSingleValue.builder()
            .value(stats.getMax())
            .name(a.getName())
            .valueAsString(stats.getMaxAsString())
            .build());
        bucket.setMin(MetricsResponseSingleValue.builder()
            .value(stats.getMin())
            .name(a.getName())
            .valueAsString(stats.getMinAsString())
            .build());
        bucket.setSum(MetricsResponseSingleValue.builder()
            .value(stats.getSum())
            .name(a.getName())
            .valueAsString(stats.getSumAsString())
            .build());
    }
}
