package org.jetlinks.community.elastic.search.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.index.mapping.IndexMappingMetadata;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.community.elastic.search.index.ElasticIndex;
import org.jetlinks.community.elastic.search.parser.QueryParamTranslateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class DefaultElasticSearchService implements ElasticSearchService {

    private final ElasticRestClient restClient;

    private final IndexOperationService indexOperationService;

    private final QueryParamTranslateService translateService;

    FluxSink<Buffer> sink;

    @Autowired
    public DefaultElasticSearchService(ElasticRestClient restClient,
                                       QueryParamTranslateService translateService,
                                       IndexOperationService indexOperationService) {
        this.restClient = restClient;
        this.translateService = translateService;
        this.indexOperationService = indexOperationService;
    }


    @Override
    public <T> Mono<PagerResult<T>> queryPager(ElasticIndex index, QueryParam queryParam, Class<T> type) {
        return query(searchRequestStructure(queryParam, index))
            .map(response -> translatePageResult(type, queryParam, response))
            .switchIfEmpty(Mono.just(PagerResult.empty()));
    }

    @Override
    public <T> Flux<T> query(ElasticIndex index, QueryParam queryParam, Class<T> type) {
        return query(searchRequestStructure(queryParam, index))
            .flatMapIterable(response -> translate(type, response));
    }

    @Override
    public Mono<Long> count(ElasticIndex index, QueryParam queryParam) {
        return countQuery(countRequestStructure(queryParam, index))
            .map(CountResponse::getCount)
            .switchIfEmpty(Mono.just(0L));
    }


    @Override
    public <T> Mono<Void> commit(ElasticIndex index, T payload) {
        return Mono.fromRunnable(() -> {
            sink.next(new Buffer(index, payload));
        });
    }

    @Override
    public <T> Mono<Void> commit(ElasticIndex index, Collection<T> payload) {
        return Mono.fromRunnable(() -> {
            for (T t : payload) {
                sink.next(new Buffer(index, t));
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        sink.complete();
    }

    @PostConstruct
    public void init() {

        FluxUtils.bufferRate(Flux.<Buffer>create(sink -> this.sink = sink),
            1000, 2000, Duration.ofSeconds(3))
            .flatMap(this::doSave)
            .doOnNext((len) ->{
                //System.out.println(len);
                log.debug("保存ES数据成功:{}", len);
            })
            .onErrorContinue((err, obj) -> {
                //打印到控制台以免递归调用ES导致崩溃
                System.err.println(org.hswebframework.utils.StringUtils.throwable2String(err));
            })
            .subscribe();

    }

    @AllArgsConstructor
    @Getter
    static class Buffer {
        ElasticIndex index;
        Object payload;
    }


    protected Mono<Integer> doSave(Collection<Buffer> buffers) {
        return Flux.fromIterable(buffers)
            .collect(Collectors.groupingBy(Buffer::getIndex))
            .map(Map::entrySet)
            .flatMapIterable(Function.identity())
            .map(entry -> {
                ElasticIndex index = entry.getKey();
                BulkRequest bulkRequest = new BulkRequest(index.getStandardIndex(), index.getStandardType());
                for (Buffer buffer : entry.getValue()) {
                    IndexRequest request = new IndexRequest();
                    Object o = JSON.toJSON(buffer.getPayload());
                    if (o instanceof Map) {
                        request.source((Map) o);
                    } else {
                        request.source(o.toString(), XContentType.JSON);
                    }
                    bulkRequest.add(request);
                }
                entry.getValue().clear();
                return bulkRequest;
            })
            .flatMap(bulkRequest ->
                Mono.<Boolean>create(sink ->
                    restClient.getWriteClient()
                        .bulkAsync(bulkRequest, RequestOptions.DEFAULT, new ActionListener<BulkResponse>() {
                            @Override
                            public void onResponse(BulkResponse responses) {
                                if (responses.hasFailures()) {
                                    sink.error(new RuntimeException("批量存储es数据失败：" + responses.buildFailureMessage()));
                                    return;
                                }
                                sink.success(!responses.hasFailures());
                            }

                            @Override
                            public void onFailure(Exception e) {
                                sink.error(e);
                            }
                        })))
            .then(Mono.just(buffers.size()));
    }

    private <T> PagerResult<T> translatePageResult(Class<T> clazz, QueryParam param, SearchResponse response) {
        long total = response.getHits().getTotalHits();
        return PagerResult.of((int) total, translate(clazz, response), param);
    }

    private <T> List<T> translate(Class<T> clazz, SearchResponse response) {
        return Arrays.stream(response.getHits().getHits())
            .map(hit -> JSON.toJavaObject(new JSONObject(hit.getSourceAsMap()), clazz))
            .collect(Collectors.toList());
    }

    private Mono<SearchResponse> query(Mono<SearchRequest> requestMono) {
        return requestMono.flatMap((request) -> Mono.create(sink -> {
                restClient.getQueryClient()
                    .searchAsync(request, RequestOptions.DEFAULT, translatorActionListener(sink));
            }
        ));
    }

    private Mono<CountResponse> countQuery(Mono<CountRequest> requestMono) {
        return requestMono.flatMap((request) -> Mono.create(sink -> {
                restClient.getQueryClient()
                    .countAsync(request, RequestOptions.DEFAULT, translatorActionListener(sink));
            }
        ));
    }

    private <T> ActionListener<T> translatorActionListener(MonoSink<T> sink) {
        return new ActionListener<T>() {
            @Override
            public void onResponse(T response) {
                sink.success(response);
            }

            @Override
            public void onFailure(Exception e) {
                if (e instanceof ElasticsearchException) {
                    if (((ElasticsearchException) e).status().getStatus() == 404) {
                        sink.success();
                        return;
                    } else if (((ElasticsearchException) e).status().getStatus() == 400) {
                        sink.error(new ElasticsearchParseException("查询参数格式错误", e));
                    }
                }
                sink.error(e);
            }
        };
    }

    private Mono<SearchRequest> searchRequestStructure(QueryParam queryParam, ElasticIndex provider) {
        return indexOperationService.getIndexMappingMetadata(provider.getStandardIndex())
            .switchIfEmpty(Mono.just(IndexMappingMetadata.getInstance(provider.getStandardIndex())))
            .map(metadata -> {
                SearchRequest request = new SearchRequest(provider.getStandardIndex())
                    .source(translateService.translate(queryParam, metadata));
                if (StringUtils.hasText(provider.getStandardType())) {
                    request.types(provider.getStandardType());
                }
                return request;
            })
            .doOnNext(searchRequest -> log.debug("查询index：{},es查询参数:{}", provider.getStandardIndex(), searchRequest.source().toString()));
    }

    private Mono<CountRequest> countRequestStructure(QueryParam queryParam, ElasticIndex provider) {
        queryParam.setPaging(false);
        return indexOperationService.getIndexMappingMetadata(provider.getStandardIndex())
            .map(metadata -> new CountRequest(provider.getStandardIndex())
                .source(translateService.translate(queryParam, metadata)))
            .doOnNext(searchRequest -> log.debug("查询index：{},es查询参数:{}", provider.getStandardIndex(), searchRequest.source().toString()));
    }
}
