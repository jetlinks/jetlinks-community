package org.jetlinks.community.elastic.search.timeseries;

import lombok.AllArgsConstructor;
import org.elasticsearch.search.aggregations.bucket.histogram.ExtendedBounds;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.TermType;
import org.jetlinks.community.elastic.search.aggreation.bucket.Bucket;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketAggregationsStructure;
import org.jetlinks.community.elastic.search.aggreation.bucket.Sort;
import org.jetlinks.community.elastic.search.aggreation.enums.BucketType;
import org.jetlinks.community.elastic.search.aggreation.enums.MetricsType;
import org.jetlinks.community.elastic.search.aggreation.enums.OrderType;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.timeseries.query.AggregationQueryParam;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
public class ESTimeSeriesService extends AbstractTimeSeriesService {

    private ESAbstractTimeSeriesManager.LocalTimeSeriesMetric localTimeSeriesMetric;

    private ElasticSearchService elasticSearchService;

    private AggregationService aggregationService;


    @Override
    public Flux<TimeSeriesData> query(QueryParam queryParam) {
        return elasticSearchService.query(
            cleverGetIndex(),
            filterAddDefaultSort(queryParam),
            Map.class)
            .map(map -> new TimeSeriesData() {

                @Override
                public long getTimestamp() {
                    return (long) map.get("timestamp");
                }

                @Override
                public Map<String, Object> getData() {
                    return map;
                }
            });
    }

    @Override
    public Mono<Integer> count(QueryParam queryParam) {
        return elasticSearchService.count(
            cleverGetIndex(),
            queryParam
        ).map(Long::intValue);
    }

    @Override
    public Flux<AggregationData> aggregation(AggregationQueryParam param) {
        BucketAggregationsStructure structure = getStandardStructure(param);
        //由于es一次性返回所有聚合查询结果，不完全支持响应式规范
        return aggregationService.bucketAggregation(
            filterAddDefaultSort(addQueryTimeRange(param.getQueryParam(), param)),
            structure,
            cleverGetIndex())
            .onErrorResume(err -> Mono.empty())
            .flatMapMany(bucketResponse -> Flux.fromIterable(new BucketsParser(bucketResponse.getBuckets()).result)
                .take(param.getLimit())
                .map(ESAggregationData::new))
            ;
    }

    @Override
    public Mono<Void> save(Publisher<TimeSeriesData> data) {
        return Flux.from(data)
            .map(timeSeriesData -> {
                Map<String, Object> map = timeSeriesData.getData();
                map.put("timestamp", timeSeriesData.getTimestamp());
                return map;
            })
            .as(stream -> elasticSearchService.commit(getDefaultIndex(), stream));
    }

    @Override
    public Mono<Void> save(TimeSeriesData data) {
        Map<String, Object> map = data.getData();
        map.put("timestamp", data.getTimestamp());
        return elasticSearchService.commit(getDefaultIndex(), map);
    }


    public void pack(BucketAggregationsStructure structureOutLayer, BucketAggregationsStructure structureInnerLayer) {
        structureOutLayer.setSubBucketAggregation(Collections.singletonList(structureInnerLayer));
    }


    protected BucketAggregationsStructure getStandardStructure(AggregationQueryParam param) {
        List<BucketAggregationsStructure> structures = new ArrayList<>();
        if (param.getGroupByTime() != null) {
            structures.add(getDateTypeStructure(param));
        }
        if (param.getGroupBy() != null && !param.getGroupBy().isEmpty()) {
            structures.addAll(getTermTypeStructures(param));
        }
        for (int i = 0; i < structures.size(); i++) {
            if (i < structures.size() - 1) {
                pack(structures.get(i), structures.get(i + 1));
            }
            if (i == structures.size() - 1) {
                structures.get(i).setSubMetricsAggregation(param.getAggColumns()
                    .stream()
                    .map(agg -> {
                        MetricsAggregationStructure metricsAggregationStructure = new MetricsAggregationStructure();
                        metricsAggregationStructure.setField(agg.getProperty());
                        metricsAggregationStructure.setName(agg.getAlias());
                        metricsAggregationStructure.setType(MetricsType.of(agg.getAggregation().name()));
                        return metricsAggregationStructure;
                    }).collect(Collectors.toList()));
            }
        }
        return structures.get(0);
    }


    protected BucketAggregationsStructure getDateTypeStructure(AggregationQueryParam param) {
        BucketAggregationsStructure structure = new BucketAggregationsStructure();
        structure.setInterval(durationFormat(param.getGroupByTime().getInterval()));
        structure.setType(BucketType.DATE_HISTOGRAM);
        structure.setFormat(param.getGroupByTime().getFormat());
        structure.setName(param.getGroupByTime().getAlias());
        structure.setField("timestamp");
        structure.setSort(Sort.desc(OrderType.KEY));
        structure.setExtendedBounds(getExtendedBounds(param));
        return structure;
    }

    protected List<BucketAggregationsStructure> getTermTypeStructures(AggregationQueryParam param) {
        return param.getGroupBy()
            .stream()
            .map(group -> {
                BucketAggregationsStructure structure = new BucketAggregationsStructure();
                structure.setType(BucketType.TERMS);
                structure.setSize(param.getLimit());
                structure.setField(group.getProperty());
                structure.setName(group.getAlias());
                return structure;
            }).collect(Collectors.toList());
    }

    protected ElasticIndex getDefaultIndex() {
        return ElasticIndex.createDefaultIndex(() -> localTimeSeriesMetric.getStandardsIndex(), () -> "_doc");
    }

    protected ElasticIndex cleverGetIndex() {
        if (localTimeSeriesMetric.isIndexHasStrategy()) {
            return ElasticIndex.createDefaultIndex(() -> localTimeSeriesMetric.getAlias().name(), () -> "_doc");
        } else {
            return ElasticIndex.createDefaultIndex(() -> localTimeSeriesMetric.getIndex(), () -> "_doc");
        }
    }


    protected static QueryParam addQueryTimeRange(QueryParam queryParam, AggregationQueryParam param) {
        queryParam.and(
            "timestamp",
            TermType.btw,
            Arrays.asList(getStartWithParam(param), param.getEndWithTime()));
        return queryParam;
    }

    protected static ExtendedBounds getExtendedBounds(AggregationQueryParam param) {
        return new ExtendedBounds(getStartWithParam(param), param.getEndWithTime());
    }

    private static long getStartWithParam(AggregationQueryParam param) {
        long startWithParam = param.getStartWithTime();
        if (param.getGroupByTime() != null && param.getGroupByTime().getInterval() != null) {
            long timeInterval = param.getGroupByTime().getInterval().toMillis() * param.getLimit();
            long tempStartWithParam = param.getEndWithTime() - timeInterval;
            startWithParam = Math.max(tempStartWithParam, startWithParam);
        }
        return startWithParam;
    }


    protected static String durationFormat(Duration duration) {
        String durationStr = duration.toString();
        if (durationStr.contains("S")) {
            return duration.toMillis() / 1000 + "s";
        } else if (!durationStr.contains("S") && durationStr.contains("M")) {
            return duration.toMinutes() + "m";
        } else if (!durationStr.contains("S") && !durationStr.contains("M")) {
            if (duration.toHours() % 24 == 0) {
                return duration.toDays() + "d";
            } else {
                return duration.toHours() + "h";
            }
        }
        throw new UnsupportedOperationException("不支持的格式化，Duration: " + duration.toString());
    }

    static class BucketsParser {

        private List<Map<String, Object>> result;

        public BucketsParser(List<Bucket> buckets) {
            result = new ArrayList<>();
            buckets.forEach(bucket -> parser(bucket, new HashMap<>()));
        }

        public void parser(Bucket bucket, Map<String, Object> fMap) {
            addBucketProperty(bucket, fMap);
            if (bucket.getBuckets() != null && !bucket.getBuckets().isEmpty()) {
                bucket.getBuckets().forEach(b -> {
                    Map<String, Object> map = new HashMap<>(fMap);
                    addBucketProperty(b, map);
                    parser(b, map);
                });
            } else {
                result.add(fMap);
            }
        }

        private void addBucketProperty(Bucket bucket, Map<String, Object> fMap) {
            fMap.put(bucket.getName(), bucket.getKey());
            fMap.putAll(bucket.toMap());
        }
    }
}
