package org.jetlinks.community.elastic.search.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.utils.time.DefaultDateFormatter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.utils.ElasticSearchConverter;
import org.jetlinks.community.elastic.search.utils.QueryParamTranslator;
import org.jetlinks.community.elastic.search.utils.ReactorActionListener;
import org.jetlinks.core.utils.FluxUtils;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.util.StringUtils;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.Consumer3;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 1.0
 **/
//@Service
@Slf4j
@DependsOn("restHighLevelClient")
@Deprecated
public class DefaultElasticSearchService implements ElasticSearchService {

    private final ElasticRestClient restClient;

    private final ElasticSearchIndexManager indexManager;

    FluxSink<Buffer> sink;

    public static final IndicesOptions indexOptions = IndicesOptions.fromOptions(
        true, true, false, false
    );

    static {
        DateFormatter.supportFormatter.add(new DefaultDateFormatter(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.+"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }

    public DefaultElasticSearchService(ElasticRestClient restClient,
                                       ElasticSearchIndexManager indexManager) {
        this.restClient = restClient;
        init();
        this.indexManager = indexManager;
    }

    @Override
    public <T> Flux<T> multiQuery(String[] index, Collection<QueryParam> queryParam, Function<Map<String, Object>, T> mapper) {
        return indexManager
            .getIndexesMetadata(index)
            .flatMap(idx -> Mono.zip(
                Mono.just(idx), getIndexForSearch(idx.getIndex())
            ))
            .take(1)
            .singleOrEmpty()
            .flatMapMany(indexMetadata -> {
                MultiSearchRequest request = new MultiSearchRequest();
                return Flux
                    .fromIterable(queryParam)
                    .flatMap(entry -> createSearchRequest(entry, index))
                    .doOnNext(request::add)
                    .then(Mono.just(request))
                    .flatMapMany(searchRequest -> ReactorActionListener
                        .<MultiSearchResponse>mono(actionListener -> {
                            restClient.getQueryClient()
                                .msearchAsync(searchRequest, RequestOptions.DEFAULT, actionListener);
                        })
                        .flatMapMany(response -> Flux.fromArray(response.getResponses()))
                        .flatMap(item -> {
                            if (item.isFailure()) {
                                log.warn(item.getFailureMessage(), item.getFailure());
                                return Mono.empty();
                            }
                            return Flux.fromIterable(translate((map) -> mapper.apply(indexMetadata.getT1().convertFromElastic(map)), item.getResponse()));
                        }))
                    ;
            });
    }

    @Override
    public <T> Flux<T> query(String index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return this
            .doQuery(new String[]{index}, queryParam)
            .flatMapMany(tp2 -> convertQueryResult(tp2.getT1(), tp2.getT2(), mapper));
    }

    @Override
    public <T> Flux<T> query(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return this
            .doQuery(index, queryParam)
            .flatMapMany(tp2 -> convertQueryResult(tp2.getT1(), tp2.getT2(), mapper));
    }

    @Override
    public <T> Mono<PagerResult<T>> queryPager(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return this
            .doQuery(index, queryParam)
            .flatMap(tp2 ->
                convertQueryResult(tp2.getT1(), tp2.getT2(), mapper)
                    .collectList()
                    .filter(CollectionUtils::isNotEmpty)
                    .map(list -> PagerResult.of((int) tp2.getT2().getHits().getTotalHits().value, list, queryParam))
            )
            .switchIfEmpty(Mono.fromSupplier(PagerResult::empty));
    }

    private <T> Flux<T> convertQueryResult(List<ElasticSearchIndexMetadata> indexList,
                                           SearchResponse response,
                                           Function<Map<String, Object>, T> mapper) {
        return Flux
            .create(sink -> {
                Map<String, ElasticSearchIndexMetadata> metadata = indexList
                    .stream()
                    .collect(Collectors.toMap(ElasticSearchIndexMetadata::getIndex, Function.identity()));
                SearchHit[] hits = response.getHits().getHits();
                for (SearchHit hit : hits) {
                    Map<String, Object> hitMap = hit.getSourceAsMap();
                    if (StringUtils.isEmpty(hitMap.get("id"))) {
                        hitMap.put("id", hit.getId());
                    }

                    sink.next(mapper
                        .apply(Optional
                            .ofNullable(metadata.get(hit.getIndex())).orElse(indexList.get(0))
                            .convertFromElastic(hitMap)));
                }
                sink.complete();
            });

    }

    private Mono<Tuple2<List<ElasticSearchIndexMetadata>, SearchResponse>> doQuery(String[] index,
                                                                                   QueryParam queryParam) {
        return indexManager
            .getIndexesMetadata(index)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(metadataList -> this
                .createSearchRequest(queryParam, metadataList)
                .flatMap(this::doSearch)
                .map(response -> Tuples.of(metadataList, response))
            ).onErrorResume(err -> {
                log.error(err.getMessage(), err);
                return Mono.empty();
            });
    }


    @Override
    public Mono<Long> count(String[] index, QueryParam queryParam) {
        QueryParam param = queryParam.clone();
        param.setPaging(false);
        return createCountRequest(param, index)
            .flatMap(this::doCount)
            .map(CountResponse::getCount)
            .defaultIfEmpty(0L)
            .onErrorReturn(err -> {
                log.error("query elastic error", err);
                return true;
            }, 0L);
    }

    @Override
    public Mono<Long> delete(String index, QueryParam queryParam) {

        return createQueryBuilder(queryParam, index)
            .flatMap(request -> ReactorActionListener
                .<BulkByScrollResponse>mono(listener ->
                    restClient
                        .getWriteClient()
                        .deleteByQueryAsync(new DeleteByQueryRequest(index)
                                .setQuery(request),
                            RequestOptions.DEFAULT, listener)))
            .map(BulkByScrollResponse::getDeleted);
    }

    @Override
    public <T> Mono<Void> commit(String index, T payload) {
        sink.next(new Buffer(index, payload));
        return Mono.empty();
    }

    @Override
    public <T> Mono<Void> commit(String index, Collection<T> payload) {
        for (T t : payload) {
            sink.next(new Buffer(index, t));
        }
        return Mono.empty();
    }

    @Override
    public <T> Mono<Void> commit(String index, Publisher<T> data) {
        return Flux.from(data)
            .flatMap(d -> commit(index, d))
            .then();
    }

    @Override
    public <T> Mono<Void> save(String index, T payload) {
        return save(index, Mono.just(payload));
    }

    @Override
    public <T> Mono<Void> save(String index, Publisher<T> data) {
        return Flux.from(data)
            .map(v -> new Buffer(index, v))
            .collectList()
            .flatMap(this::doSave)
            .then();
    }

    @Override
    public <T> Mono<Void> save(String index, Collection<T> payload) {
        return save(index, Flux.fromIterable(payload));
    }

    @PreDestroy
    public void shutdown() {
        sink.complete();
    }

    //@PostConstruct
    public void init() {
        //最小间隔
        int flushRate = Integer.getInteger("elasticsearch.buffer.rate", 1000);
        //缓冲最大数量
        int bufferSize = Integer.getInteger("elasticsearch.buffer.size", 3000);
        //缓冲超时时间
        Duration bufferTimeout = Duration.ofSeconds(Integer.getInteger("elasticsearch.buffer.timeout", 3));
        //缓冲背压
        int bufferBackpressure = Integer.getInteger("elasticsearch.buffer.backpressure", 64);

        FluxUtils.bufferRate(
            Flux.<Buffer>create(sink -> this.sink = sink),
            flushRate,
            bufferSize,
            bufferTimeout)
            .onBackpressureBuffer(bufferBackpressure,
                drop -> System.err.println("无法处理更多索引请求!"),
                BufferOverflowStrategy.DROP_OLDEST)
            .parallel()
            .runOn(Schedulers.newParallel("elasticsearch-writer"))
            .flatMap(buffers -> {
                long time = System.currentTimeMillis();
                return this
                    .doSave(buffers)
                    .doOnNext((len) -> log.trace("保存ElasticSearch数据成功,数量:{},耗时:{}ms", len, (System.currentTimeMillis() - time)))
                    .onErrorContinue((err, obj) -> {
                        //这里的错误都输出到控制台,输入到slf4j可能会造成日志递归.
                        System.err.println("保存ElasticSearch数据失败:\n" + org.hswebframework.utils.StringUtils.throwable2String(err));
                    });
            })
            .subscribe();
    }

    @AllArgsConstructor
    @Getter
    static class Buffer {
        String index;
        Object payload;
    }


    private Mono<String> getIndexForSave(String index) {
        return indexManager
            .getIndexStrategy(index)
            .map(strategy -> strategy.getIndexForSave(index));

    }

    private Mono<String> getIndexForSearch(String index) {
        return indexManager
            .getIndexStrategy(index)
            .map(strategy -> strategy.getIndexForSearch(index));

    }

    protected Mono<Integer> doSave(Collection<Buffer> buffers) {
        return Flux.fromIterable(buffers)
            .groupBy(Buffer::getIndex)
            .flatMap(group -> {
                String index = group.key();
                return this.getIndexForSave(index)
                    .zipWith(indexManager.getIndexMetadata(index))
                    .flatMapMany(tp2 ->
                        group.map(buffer -> {
                            Map<String, Object> data = FastBeanCopier.copy(buffer.getPayload(), HashMap::new);

                            IndexRequest request;
                            if (data.get("id") != null) {
                                request = new IndexRequest(tp2.getT1(), "_doc", String.valueOf(data.get("id")));
                            } else {
                                request = new IndexRequest(tp2.getT1(), "_doc");
                            }
                            request.source(tp2.getT2().convertToElastic(data));
                            return request;
                        }));
            })
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(lst -> {
                BulkRequest request = new BulkRequest();
                lst.forEach(request::add);
                return ReactorActionListener.<BulkResponse>mono(listener ->
                    restClient.getWriteClient().bulkAsync(request, RequestOptions.DEFAULT, listener))
                    .doOnNext(this::checkResponse);
            })
            .thenReturn(buffers.size());
    }

    @SneakyThrows
    protected void checkResponse(BulkResponse response) {
        if (response.hasFailures()) {
            for (BulkItemResponse item : response.getItems()) {
                if (item.isFailed()) {
                    throw item.getFailure().getCause();
                }
            }
        }
    }

    private <T> List<T> translate(Function<Map<String, Object>, T> mapper, SearchResponse response) {
        return Arrays.stream(response.getHits().getHits())
            .map(hit -> {
                Map<String, Object> hitMap = hit.getSourceAsMap();
                if (StringUtils.isEmpty(hitMap.get("id"))) {
                    hitMap.put("id", hit.getId());
                }
                return mapper.apply(hitMap);
            })
            .collect(Collectors.toList());
    }

    private Mono<SearchResponse> doSearch(SearchRequest request) {
        return this
            .execute(request, restClient.getQueryClient()::searchAsync)
            .onErrorResume(err -> {
                log.error("query elastic error", err);
                return Mono.empty();
            });
    }

    private <REQ, RES> Mono<RES> execute(REQ request, Consumer3<REQ, RequestOptions, ActionListener<RES>> function4) {
        return ReactorActionListener.mono(actionListener -> function4.accept(request, RequestOptions.DEFAULT, actionListener));
    }

    private Mono<CountResponse> doCount(CountRequest request) {
        return this
            .execute(request, restClient.getQueryClient()::countAsync)
            .onErrorResume(err -> {
                log.error("query elastic error", err);
                return Mono.empty();
            });
    }

    protected Mono<SearchRequest> createSearchRequest(QueryParam queryParam, String... indexes) {
        return indexManager
            .getIndexesMetadata(indexes)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> createSearchRequest(queryParam, list));
    }

    protected Mono<SearchRequest> createSearchRequest(QueryParam queryParam, List<ElasticSearchIndexMetadata> indexes) {

        SearchSourceBuilder builder = ElasticSearchConverter.convertSearchSourceBuilder(queryParam, indexes.get(0));
        return Flux.fromIterable(indexes)
            .flatMap(index -> getIndexForSearch(index.getIndex()))
            .collectList()
            .map(indexList ->
                new SearchRequest(indexList.toArray(new String[0]))
                    .source(builder)
                    .indicesOptions(indexOptions)
                    .types("_doc"));
    }

    protected Mono<QueryBuilder> createQueryBuilder(QueryParam queryParam, String index) {
        return indexManager
            .getIndexMetadata(index)
            .map(metadata -> QueryParamTranslator.createQueryBuilder(queryParam, metadata))
            .switchIfEmpty(Mono.fromSupplier(() -> QueryParamTranslator.createQueryBuilder(queryParam, null)));
    }

    protected Mono<CountRequest> createCountRequest(QueryParam queryParam, List<ElasticSearchIndexMetadata> indexes) {
        QueryParam tempQueryParam = queryParam.clone();
        tempQueryParam.setPaging(false);
        tempQueryParam.setSorts(Collections.emptyList());

        SearchSourceBuilder builder = ElasticSearchConverter.convertSearchSourceBuilder(queryParam, indexes.get(0));
        return Flux.fromIterable(indexes)
            .flatMap(index -> getIndexForSearch(index.getIndex()))
            .collectList()
            .map(indexList -> new CountRequest(indexList.toArray(new String[0])).source(builder));
    }

    private Mono<CountRequest> createCountRequest(QueryParam queryParam, String... index) {
        return indexManager
            .getIndexesMetadata(index)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> createCountRequest(queryParam, list));
    }
}
