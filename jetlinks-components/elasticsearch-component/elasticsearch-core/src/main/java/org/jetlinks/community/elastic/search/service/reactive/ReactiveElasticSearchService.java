package org.jetlinks.community.elastic.search.service.reactive;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchItem;
import co.elastic.clients.elasticsearch.core.msearch.MultiSearchResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.opentelemetry.api.common.AttributeKey;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.configure.cluster.Cluster;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.buffer.*;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.utils.QueryParamTranslator;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.community.utils.SystemUtils;
import org.reactivestreams.Publisher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 响应式ElasticSearchService
 *
 * @author zhouhao
 * @see ReactiveElasticsearchClient
 * @since 1.0
 **/
@Slf4j
public class ReactiveElasticSearchService implements ElasticSearchService, CommandLineRunner {

    static AttributeKey<Long> BUFFER_SIZE = AttributeKey.longKey("bufferSize");

    @Getter
    private final ReactiveElasticsearchClient restClient;
    @Getter
    private final ElasticSearchIndexManager indexManager;


    private PersistenceBuffer<Buffer> writer;

    @Getter
    @Setter
    private ElasticSearchBufferProperties buffer;

    public ReactiveElasticSearchService(ReactiveElasticsearchClient restClient,
                                        ElasticSearchIndexManager indexManager) {
        this(restClient, indexManager, new ElasticSearchBufferProperties());
    }

    public ReactiveElasticSearchService(ReactiveElasticsearchClient restClient,
                                        ElasticSearchIndexManager indexManager,
                                        ElasticSearchBufferProperties buffer) {
        this.restClient = restClient;
        this.indexManager = indexManager;
        this.buffer = buffer;
        init();
    }

    @Override
    public <T> Flux<T> multiQuery(String[] index, Collection<QueryParam> queryParam, Function<Map<String, Object>, T> mapper) {
        if (index.length == 1 && queryParam.size() == 1) {
            return query(index[0], queryParam.iterator().next(), mapper);
        }
        return indexManager
            .getIndexesMetadata(index)
            .flatMap(idx -> Mono.zip(
                Mono.just(idx), getIndexForSearch(idx.getIndex())
            ))
            .take(1)
            .singleOrEmpty()
            .flatMapMany(indexMetadata -> restClient
                .execute(client -> client
                    .msearch(b -> {
                        for (QueryParam param : queryParam) {
                            b.searches(s -> s
                                .header(header -> header
                                    .index(Arrays.asList(index))
                                    .ignoreUnavailable(true)
                                    .allowNoIndices(true))
                                .body(q -> {
                                    QueryParamTranslator.convertSearchRequestBuilder(q, param, indexMetadata.getT1());
                                    return q;
                                }));
                        }

                        return b;
                    }, Map.class)
                )
                .flatMapMany(response -> translate((map) -> mapper
                    .apply(indexMetadata.getT1().convertFromElastic(map)), response.responses())));
    }

    public <T> Flux<T> query(String index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        return this.query(new String[]{index}, queryParam, mapper);
    }

    public <T> Flux<T> query(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        if (queryParam.isPaging()) {
            return this
                .doQuery(index, queryParam)
                .flatMapMany(tp2 -> convertQueryResult(tp2.getT1(), tp2.getT2(), mapper));
        }
        return this.doScrollQuery(index, queryParam, mapper);
    }

    @Override
    public <T> Mono<PagerResult<T>> queryPager(String[] index, QueryParam queryParam, Function<Map<String, Object>, T> mapper) {
        if (!queryParam.isPaging()) {
            return Mono
                .zip(
                    this.count(index, queryParam),
                    this.query(index, queryParam, mapper).collectList(),
                    (total, list) -> PagerResult.of(total.intValue(), list, queryParam)
                )
                .switchIfEmpty(Mono.fromSupplier(PagerResult::empty));
        }
        return this
            .doQuery(index, queryParam)
            .flatMap(tp2 -> this
                .convertQueryResult(tp2.getT1(), tp2.getT2(), mapper)
                .collectList()
                .filter(CollectionUtils::isNotEmpty)
                .map(list -> PagerResult.of((int) tp2
                    .getT2()
                    .hits().total().value(), list, queryParam))
            )
            .switchIfEmpty(Mono.fromSupplier(PagerResult::empty));
    }

    private <T> Flux<T> convertQueryResult(List<ElasticSearchIndexMetadata> indexList,
                                           SearchResponse<Map> response,
                                           Function<Map<String, Object>, T> mapper) {
        Map<String, ElasticSearchIndexMetadata> metadata = indexList
            .stream()
            .collect(Collectors.toMap(ElasticSearchIndexMetadata::getIndex, Function.identity()));

        return Flux
            .fromIterable(response.hits().hits())
            .mapNotNull(hit -> {
                Map<String, Object> hitMap = hit.source();
                if (hitMap == null) {
                    return null;
                }
                hitMap.putIfAbsent("id", hit.id());
                return mapper
                    .apply(Optional
                               .ofNullable(metadata.get(hit.index())).orElse(indexList.get(0))
                               .convertFromElastic(hitMap));
            });

    }

    private Mono<Tuple2<List<ElasticSearchIndexMetadata>, SearchResponse<Map>>>
    doQuery(String[] index,
            QueryParam queryParam) {
        return indexManager
            .getIndexesMetadata(index)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(metadataList -> this
                .createSearchRequest(queryParam, metadataList)
                .flatMap(request -> restClient
                    .execute(c -> c.search(request, Map.class)))
                .map(response -> Tuples.of(metadataList, response))
            )
            ;
    }

    private <T> Flux<T> doScrollQuery(String[] index,
                                      QueryParam queryParam,
                                      Function<Map<String, Object>, T> mapper) {
        Time time = Time.of(t -> t.time("10m"));

        return indexManager
            .getIndexesMetadata(index)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMapMany(metadataList -> this
                .createSearchRequest(queryParam.clone().noPaging(), metadataList)
                .flatMapMany(search -> new ScrollingFlux<T>(
                    mapper,
                    restClient,
                    time,
                    builder -> {
                        builder.index(search.index());
                        builder.query(search.query());
                        builder.sort(search.sort());
                        builder.size(getNoPagingPageSize(queryParam));
                    }))
            );
    }

    private int getNoPagingPageSize(QueryParam param) {
        return Math.max(10000, param.getPageSize());
    }

    @Override
    public Mono<Long> count(String[] index, QueryParam queryParam) {
        QueryParam param = queryParam.clone();
        param.setPaging(false);
        return this
            .createSearchRequest(param, index)
            .flatMap(this::doCount)
            .defaultIfEmpty(0L);
    }

    @Override
    public Mono<Long> delete(String index, QueryParam queryParam) {
        return this
            .getIndexForSearch(index)
            .flatMap(inx -> this
                .createSearchRequest(queryParam, inx)
                .flatMap(request -> restClient.execute(
                    client -> client
                        .deleteByQuery(q -> q.query(request.query())
                                             .index(request.index()))
                        .deleted()))
                .defaultIfEmpty(0L));
    }

    private Map<String, Object> convertToMap(Object payload) {
        @SuppressWarnings("all")
        Map<String, Object> map = payload instanceof Map
            ? ((Map) payload) :
            FastBeanCopier.copy(payload, HashMap::new);

        return map;
    }

    @Override
    public <T> Mono<Void> commit(String index, T payload) {
        return writer
            .writeAsync(Buffer.of(index, convertToMap(payload)));
    }

    @Override
    public <T> Mono<Void> commit(String index, Collection<T> payload) {
        return writer
            .writeAsync(Collections2.transform(payload, t -> Buffer.of(index, convertToMap(t))));
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
                   .map(v -> Buffer.of(index, convertToMap(v)))
                   .buffer(buffer.getSize())
                   .flatMap(this::doSave)
                   .then();
    }

    @Override
    public <T> Mono<Void> save(String index, Collection<T> payload) {
        return save(index, Flux.fromIterable(payload));
    }

    @Override
    public void run(String... args) {
        startup();
    }

    public void shutdown0() {
        writer.dispose();
    }

    public void shutdown() {
        writer.stop();
    }

    public void startup() {
        //spring 启动后更新配置信息
        writer
            .settings(bufferSettings -> bufferSettings.properties(buffer))
            .start();

        //最后 shutdown
        SpringApplication
            .getShutdownHandlers()
            .add(this::shutdown0);
    }

    private void init() {

        writer = new PersistenceBuffer<>(
            BufferSettings.create(Cluster.safeId() + ".queue", buffer),
            Buffer::new,
            this::doSaveBuffer)
            .name("elasticsearch")
            .retryWhenError(e -> {
                if (e instanceof ElasticsearchException elasticsearchException) {
                    if (elasticsearchException.status() == 502) {
                        return true;
                    }
                }
                return ErrorUtils.hasException(e, WebClientException.class)
                    || ErrorUtils.hasException(e, IOException.class);
            });

        writer.init();

    }

    public Mono<Boolean> doSaveBuffer(Collection<Buffered<Buffer>> bufferFlux,
                                      PersistenceBuffer.FlushContext<Buffer> context) {
        List<Buffered<Buffer>> list = bufferFlux instanceof List
            ? ((List<Buffered<Buffer>>) bufferFlux)
            : new ArrayList<>(bufferFlux);
        int size = list.size();
        return this
            .doSave0(Collections2.transform(list, Buffered::getData))
            .map(response -> {
                boolean hasError = false;
                List<BulkResponseItem> arr = response.items();
                Set<String> errors = null;
                int responseSize = arr.size();
                //响应数量不一致?
                if (responseSize != size) {
                    log.warn("ElasticSearch response item size not equals to buffer size," +
                                 " response size:{}, buffer size:{}",
                             responseSize,
                             size);
                }
                for (int i = 0; i < responseSize; i++) {
                    BulkResponseItem item = arr.get(i);
                    Buffered<Buffer> buffered = size > i ? list.get(i) : null;
                    HttpStatus status = HttpStatus.resolve(item.status());
                    if ((status == null || !status.is2xxSuccessful())) {
                        hasError = true;
                        if (null != item.error()) {
                            context.error(new BusinessException.NoStackTrace(item.error().reason()));
                        } else {
                            context.error(new BusinessException.NoStackTrace(item.toString()));
                        }
                        if (log.isInfoEnabled()) {
                            String msg = item.error().reason();
                            if (errors == null) {
                                errors = new HashSet<>();
                            }
                            if (msg == null || errors.add(msg)) {
                                log.info("write elasticsearch data [{}] failed: {}",
                                         buffered,
                                         item);
                            }
                        }
                        //失败
                        if (buffered != null) {
                            if (isDead(buffered, item)) {
                                //标记dead
                                buffered.dead();
                            } else {
                                //标记重试
                                buffered.retry(true);
                            }
                        }
                        continue;
                    }
                    //成功,标记此条数据不重试.
                    if (buffered != null) {
                        buffered.retry(false);
                    }
                }
                //有任何错误,则触发重试
                return hasError;
            });

    }

    private static final Set<Integer> deadStatus = Sets.newHashSet(403, 400, 401, 404, 405);

    private boolean isDead(Buffered<Buffer> buffered, BulkResponseItem response) {
        return buffer.isExceededRetryCount(buffered.getRetryTimes()) ||
            //快速失败,不再重试
            deadStatus.contains(response.status());
    }

    @Getter
    public static class Buffer implements Externalizable, MemoryUsage, Jsonable {
        private static final long serialVersionUID = 1;

        String index;
        String id;
        Object payload;

        @Override
        public JSONObject toJson() {
            JSONObject object = new JSONObject();
            object.put("index", index);
            object.put("id", id);
            object.put("payload", payload);
            return object;
        }

        @SneakyThrows
        public static Buffer of(String index, Map<String, Object> data) {
            Buffer buffer = new Buffer();
            buffer.index = index;
            Object id = data.get("id");
            buffer.id = id == null ? null : String.valueOf(id);
            buffer.payload = data;
            return buffer;
        }

        void release() {

        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(index);
            SerializeUtils.writeNullableUTF(id, out);
            SerializeUtils.writeObject(payload, out);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            index = in.readUTF();
            id = SerializeUtils.readNullableUTF(in);
            payload = SerializeUtils.readObject(in);
        }

        @Override
        public int usage() {
            return 1024;
        }

        @Override
        public String toString() {
            return "index:" + index + ",id:" + id;
        }
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

    protected Mono<BulkResponse> doSave0(Collection<Buffer> buffers) {
        long size = buffers.size();
        return Flux
            .fromIterable(buffers)
            .groupBy(Buffer::getIndex, Integer.MAX_VALUE)
            .flatMap(group -> {
                String index = group.key();
                return this
                    .getIndexForSave(index)
                    .flatMapMany(realIndex -> group
                        .map(buffer -> IndexOperation
                            .of(b -> {
                                if (buffer.id != null && !this.buffer.isIgnoreDocId()) {
                                    b.id(buffer.id);
                                }
                                b.index(realIndex);
                                b.document(buffer.payload);
                                return b;
                            })));
            })
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(lst -> restClient
                .execute(c -> c.bulk(builder -> {
                    builder
                        .refresh(Refresh.True)
                        .operations(Lists.transform(
                            lst, v -> BulkOperation.of(b -> b.index(v))
                        ));
                    return builder;
                })))
            .as(MonoTracer.create(
                "/_elasticsearch/save-buffer",
                builder -> builder.setAttribute(BUFFER_SIZE, size)));
    }

    protected Mono<Integer> doSave(Collection<Buffer> buffers) {
        return doSave0(buffers)
            .doOnError((err) -> {
                //这里的错误都输出到控制台,输入到slf4j可能会造成日志递归.
                SystemUtils.printError("保存ElasticSearch数据失败:\n%s", () -> new Object[]{
                    StringUtils.throwable2String(err)
                });
            })
            .map(response -> {
                int success = 0;
                for (BulkResponseItem item : response.items()) {
                    if (item.error() != null) {
                        success++;
                    }
                }
                return success;
            })
            ;
    }

    private Exception createError(ErrorResponse response) {

        throw new ElasticsearchException(null, response);
    }


    @SuppressWarnings("all")
    private <T> Flux<T> translate(Function<Map<String, Object>, T> mapper,
                                  HitsMetadata<Map> hits) {
        return Flux.create(sink -> {
            for (Hit<Map> hit : hits.hits()) {
                if (sink.isCancelled()) {
                    break;
                }
                Map<String, Object> hitMap = hit.source();
                if (ObjectUtils.isEmpty(hitMap.get("id"))) {
                    hitMap.put("id", hit.id());
                }
                sink.next(mapper.apply(hitMap));
            }
            sink.complete();
        });
    }

    @SuppressWarnings("all")
    private <T> Flux<T> translate(Function<Map<String, Object>, T> mapper,
                                  List<MultiSearchResponseItem<Map>> response) {

        return Flux.create(sink -> {
            for (MultiSearchResponseItem<Map> mapMultiSearchResponseItem : response) {
                if (mapMultiSearchResponseItem.isFailure()) {
                    sink.error(createError(mapMultiSearchResponseItem.failure()));
                    return;
                }
                MultiSearchItem<Map> item = mapMultiSearchResponseItem.result();
                if (sink.isCancelled()) {
                    break;
                }
                for (Hit<Map> hit : item.hits().hits()) {
                    if (sink.isCancelled()) {
                        break;
                    }

                    Map<String, Object> hitMap = hit.source();
                    if (ObjectUtils.isEmpty(hitMap.get("id"))) {
                        hitMap.put("id", hit.id());
                    }
                    sink.next(mapper.apply(hitMap));
                }
            }
            sink.complete();
        });
    }

    private Mono<Long> doCount(SearchRequest request) {
        return restClient
            .execute(c -> c
                .count(ct -> {
                    ct.index(request.index())
                      .query(request.query());
                    return ct;
                }).count());
    }

    protected Mono<SearchRequest> createSearchRequest(QueryParam queryParam,
                                                      String... indexes) {
        return indexManager
            .getIndexesMetadata(indexes)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(list -> createSearchRequest(queryParam, list));
    }

    protected int computeTrackHits(QueryParam param) {
        if (param instanceof QueryParamEntity p) {
            if (p.getTotal() != null) {
                return 0;
            }
        }
        return param.isPaging() ? Integer.MAX_VALUE : 0;
    }

    protected Mono<SearchRequest> createSearchRequest(QueryParam queryParam, List<ElasticSearchIndexMetadata> indexes) {

        return Flux
            .fromIterable(indexes)
            .flatMap(index -> getIndexForSearch(index.getIndex()))
            .collectList()
            .map(indexList -> SearchRequest.of(
                builder -> {
                    builder.ignoreUnavailable(true);
                    builder.allowNoIndices(true);
                    int track = computeTrackHits(queryParam);
                    builder.trackTotalHits(TrackHits.of(b -> track <= 0 ? b.enabled(false) : b.count(track)));
                    builder.index(indexList);
                    return QueryParamTranslator.convertSearchRequestBuilder(builder, queryParam, indexes.get(0));
                }));
    }


}
