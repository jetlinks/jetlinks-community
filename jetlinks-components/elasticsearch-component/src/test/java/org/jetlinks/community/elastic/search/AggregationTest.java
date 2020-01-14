package org.jetlinks.community.elastic.search;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.elastic.search.aggreation.bucket.Bucket;
import org.jetlinks.community.elastic.search.aggreation.bucket.Sort;
import org.jetlinks.community.elastic.search.aggreation.bucket.BucketAggregationsStructure;
import org.jetlinks.community.elastic.search.aggreation.enums.BucketType;
import org.jetlinks.community.elastic.search.aggreation.enums.MetricsType;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsAggregationStructure;
import org.jetlinks.community.elastic.search.aggreation.metrics.MetricsResponseSingleValue;
import org.jetlinks.community.elastic.search.index.DefaultIndexOperationService;
import org.jetlinks.community.elastic.search.index.mapping.IndicesMappingCenter;
import org.jetlinks.community.elastic.search.service.AggregationService;
import org.jetlinks.community.elastic.search.aggreation.DefaultAggregationService;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.elastic.search.parser.DefaultLinkTypeParser;
import org.jetlinks.community.elastic.search.parser.DefaultQueryParamTranslateService;
import org.jetlinks.community.elastic.search.parser.QueryParamTranslateService;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class AggregationTest {

    private ElasticRestClient client;

    private AggregationService aggregationService;

    private QueryParamTranslateService translateService;

    private IndexOperationService indexOperationService;


    @BeforeEach
    public void before() {
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http")));
        client = new ElasticRestClient(restHighLevelClient,restHighLevelClient);
        translateService = new DefaultQueryParamTranslateService(new DefaultLinkTypeParser());
        indexOperationService = new DefaultIndexOperationService(client, new IndicesMappingCenter());
        aggregationService = new DefaultAggregationService(indexOperationService, client, translateService);
    }

    @Test
    @SneakyThrows
    public void minTest() {
        MetricsAggregationStructure structure = new MetricsAggregationStructure();
        structure.setField("lineNumber");
        structure.setType(MetricsType.MIN);
        aggregationService.metricsAggregation(
                QueryParamEntity.of(), structure,
                ElasticIndex.createDefaultIndex(() -> "system_log", () -> "doc"))
                .doOnNext(metricsResponse -> {
                    log.info("lineNumber 最小值结果:{}", metricsResponse.getSingleResult().getValueAsString());
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @SneakyThrows
    public void termsTest() {
        BucketAggregationsStructure structure = new BucketAggregationsStructure();
        structure.setField("lineNumber");
        structure.setType(BucketType.TERMS);
        structure.setSort(Sort.asc());
        structure.setSize(2);
        aggregationService.bucketAggregation(
                QueryParamEntity.of(), structure,
                ElasticIndex.createDefaultIndex(() -> "system_log", () -> "doc"))
                .doOnNext(bucketResponse -> {
                    log.info("lineNumber terms聚合结果:{}", JSON.toJSONString(bucketResponse, SerializerFeature.PrettyFormat));
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @SneakyThrows
    public void termsNestMetricsTest() {
        BucketAggregationsStructure structureTime = new BucketAggregationsStructure();
        structureTime.setField("name");
        structureTime.setType(BucketType.TERMS);
        structureTime.setSubMetricsAggregation(Collections.singletonList(MetricsAggregationStructure.builder()
                .field("value")
                .type(MetricsType.AVG)
                .build()));
        BucketAggregationsStructure structure = new BucketAggregationsStructure();
        structure.setField("@timestamp");
        structure.setType(BucketType.DATE_HISTOGRAM);
        structure.setFormat("yyyy-MM-dd");
        structure.setInterval("1d");
        structure.setSort(Sort.asc());
        structure.setSubBucketAggregation(Collections.singletonList(structureTime));
        aggregationService.bucketAggregation(
                QueryParamEntity.of("id.keyword", "Metaspace"), structure,
                ElasticIndex.createDefaultIndex(() -> "metrics-2019-12", () -> ""))
                .doOnNext(bucketResponse -> {

                    bucketResponse.getBuckets()
                            .forEach(bucket -> {
                                Map<String, Double> map = bucket.getBuckets()
                                        .stream()
                                        .collect(Collectors.toMap(Bucket::getKey, b-> b.getAvg().getValue()));

                                Double committed = map.get("jvm_memory_committed");
                                Double used = map.get("jvm_memory_used");
                                if (committed != null && used != null) {
                                    double result = committed / (used + committed);
                                    bucket.setAvg(MetricsResponseSingleValue.builder()
                                            .value(result)
                                            .valueAsString(String.valueOf(result))
                                            .build());
                                } else {
                                    bucket.setAvg(MetricsResponseSingleValue.builder()
                                            .value(0)
                                            .valueAsString("0")
                                            .build());
                                    log.error("获取jvm内存使用率异常, jvm可用内存:{}, jvm已使用内存:{},key:{}", committed, used, bucket.getKey());
                                }
                            });
                    log.info("lineNumber terms聚合结果:{}", JSON.toJSONString(bucketResponse, SerializerFeature.PrettyFormat));
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @SneakyThrows
    public void termsNestBucketTest() {
        BucketAggregationsStructure structure = new BucketAggregationsStructure();
        structure.setField("lineNumber");
        structure.setType(BucketType.TERMS);
        structure.setSort(Sort.asc());
        structure.setSubBucketAggregation(Arrays.asList(BucketAggregationsStructure.builder()
                .field("createTime")
                .type(BucketType.TERMS)
                .build()));
        aggregationService.bucketAggregation(
                QueryParamEntity.of(), structure,
                ElasticIndex.createDefaultIndex(() -> "system_log", () -> "doc"))
                .doOnNext(bucketResponse -> {
                    log.info("lineNumber terms聚合结果:{}", JSON.toJSONString(bucketResponse, SerializerFeature.PrettyFormat));
                })
                .as(StepVerifier::create)
                .expectNextCount(1)
                .verifyComplete();
    }
}
