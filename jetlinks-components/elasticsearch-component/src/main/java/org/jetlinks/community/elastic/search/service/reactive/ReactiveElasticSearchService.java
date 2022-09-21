package org.jetlinks.community.elastic.search.service.reactive;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.MultiSearchRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.utils.time.DefaultDateFormatter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.buffer.BufferProperties;
import org.jetlinks.community.buffer.BufferSettings;
import org.jetlinks.community.buffer.MemoryUsage;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexManager;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.elastic.search.utils.ElasticSearchConverter;
import org.jetlinks.community.elastic.search.utils.QueryParamTranslator;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.community.utils.SystemUtils;
import org.jetlinks.core.utils.SerializeUtils;
import org.reactivestreams.Publisher;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 响应式ElasticSearchService
 *
 * @author zhouhao
 * @see ReactiveElasticsearchClient
 * @since 1.0
 **/
@Slf4j
@DependsOn("reactiveElasticsearchClient")
@ConfigurationProperties(prefix = "elasticsearch")
@Service
public class ReactiveElasticSearchService implements ElasticSearchService {

    @Getter
    @Setter
    private BufferConfig buffer = new BufferConfig();

    @Getter
    private final ReactiveElasticsearchClient restClient;
    @Getter
    private final ElasticSearchIndexManager indexManager;

    public static final IndicesOptions indexOptions = IndicesOptions.fromOptions(
        true, true, false, false
    );

    static {
        DateFormatter.supportFormatter.add(new DefaultDateFormatter(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.+"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        DateFormatter.supportFormatter.add(new DefaultDateFormatter(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.+"), "yyyy-MM-dd HH:mm:ss.SSS"));
    }

    private PersistenceBuffer<Buffer> writer;

    public ReactiveElasticSearchService(ReactiveElasticsearchClient restClient,
                                        ElasticSearchIndexManager indexManager) {
        this.restClient = restClient;
        this.indexManager = indexManager;
        init();
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
                    .flatMapMany(searchRequest -> restClient
                        .multiSearch(searchRequest)
                        .flatMapMany(response -> Flux.fromArray(response.getResponses()))
                        .flatMap(item -> {
                            if (item.isFailure()) {
                                log.warn(item.getFailureMessage(), item.getFailure());
                                return Mono.empty();
                            }
                            return Flux
                                .fromIterable(translate((map) -> mapper
                                    .apply(indexMetadata.getT1().convertFromElastic(map)), item.getResponse()));
                        }))
                    ;
            });
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
        return this
            .doScrollQuery(index, queryParam)
            .flatMap(tp2 -> convertQueryHit(tp2.getT1(), tp2.getT2(), mapper));
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
                    .getHits()
                    .getTotalHits().value, list, queryParam))
            )
            .switchIfEmpty(Mono.fromSupplier(PagerResult::empty));
    }

    private <T> Flux<T> convertQueryResult(List<ElasticSearchIndexMetadata> indexList,
                                           SearchResponse response,
                                           Function<Map<String, Object>, T> mapper) {
        Map<String, ElasticSearchIndexMetadata> metadata = indexList
            .stream()
            .collect(Collectors.toMap(ElasticSearchIndexMetadata::getIndex, Function.identity()));

        return Flux
            .fromIterable(response.getHits())
            .mapNotNull(hit -> {
                Map<String, Object> hitMap = hit.getSourceAsMap();
                hitMap.putIfAbsent("id", hit.getId());
                return mapper
                    .apply(Optional
                               .ofNullable(metadata.get(hit.getIndex())).orElse(indexList.get(0))
                               .convertFromElastic(hitMap));
            });

    }

    private <T> Flux<T> convertQueryHit(List<ElasticSearchIndexMetadata> indexList,
                                        SearchHit searchHit,
                                        Function<Map<String, Object>, T> mapper) {
        Map<String, ElasticSearchIndexMetadata> metadata = indexList
            .stream()
            .collect(Collectors.toMap(ElasticSearchIndexMetadata::getIndex, Function.identity()));

        return Flux
            .just(searchHit)
            .mapNotNull(hit -> {
                Map<String, Object> hitMap = hit.getSourceAsMap();
                hitMap.putIfAbsent("id", hit.getId());
                return mapper
                    .apply(Optional
                               .ofNullable(metadata.get(hit.getIndex())).orElse(indexList.get(0))
                               .convertFromElastic(hitMap));
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
                .flatMap(restClient::searchForPage)
                .map(response -> Tuples.of(metadataList, response))
            )
            ;
    }

    private Flux<Tuple2<List<ElasticSearchIndexMetadata>, SearchHit>> doScrollQuery(String[] index,
                                                                                    QueryParam queryParam) {
        return indexManager
            .getIndexesMetadata(index)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMapMany(metadataList -> this
                .createSearchRequest(queryParam.clone().noPaging(), metadataList)
                .doOnNext(search -> search.source().size(getNoPagingPageSize(queryParam)))
                .flatMapMany(restClient::scroll)
                .map(searchHit -> Tuples.of(metadataList, searchHit))
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
                .createQueryBuilder(queryParam, index)
                .flatMap(request -> restClient.deleteBy(delete -> delete.setQuery(request).indices(inx)))
                .map(BulkByScrollResponse::getDeleted))
            .defaultIfEmpty(0L);
    }

    private boolean checkWritable(String index) {
        return true;
    }

    @Override
    public <T> Mono<Void> commit(String index, T payload) {
        if (checkWritable(index)) {
            writer.write(Buffer.of(index, payload));
        }
        return Mono.empty();
    }

    @Override
    public <T> Mono<Void> commit(String index, Collection<T> payload) {
        if (checkWritable(index)) {
            for (T t : payload) {
                writer.write(Buffer.of(index, t));
            }
        }
        return Mono.empty();
    }

    @Override
    public <T> Mono<Void> commit(String index, Publisher<T> data) {
        if (!checkWritable(index)) {
            return Mono.empty();
        }
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
                   .map(v -> Buffer.of(index, v))
                   .buffer(buffer.getSize())
                   .flatMap(this::doSave)
                   .then();
    }

    @Override
    public <T> Mono<Void> save(String index, Collection<T> payload) {
        return save(index, Flux.fromIterable(payload));
    }

    @PreDestroy
    public void shutdown() {
        writer.dispose();
    }

    @Getter
    @Setter
    public static class BufferConfig extends BufferProperties {
        public BufferConfig() {
            //固定缓冲文件目录
            setFilePath("./data/elasticsearch-buffer");
            setSize(3000);
        }

        private boolean refreshWhenWrite = false;
    }

    @PostConstruct
    public void reset() {
        //spring 启动后更新配置信息
        writer.settings(bufferSettings -> bufferSettings.properties(buffer));
    }

    private void init() {

        writer = new PersistenceBuffer<>(
            BufferSettings.create("writer.queue", buffer),
            Buffer::new,
            this::doSaveBuffer)
            .name("elasticsearch")
            .retryWhenError(e -> {
                if (e instanceof ElasticsearchException) {
                    ElasticsearchException elasticsearchException = (ElasticsearchException) e;
                    if (elasticsearchException.status() == RestStatus.BAD_GATEWAY) {
                        return true;
                    }
                }
                return ErrorUtils.hasException(e, WebClientException.class)
                    || ErrorUtils.hasException(e, IOException.class);
            });

        writer.start();

    }

    public Mono<Boolean> doSaveBuffer(Flux<Buffer> bufferFlux) {
        return bufferFlux
            .collectList()
            .flatMap(this::doSave)
            .subscribeOn(Schedulers.parallel())
            .map(i -> i == 0);
    }

    @Getter
    public static class Buffer implements Externalizable, MemoryUsage {
        private static final long serialVersionUID = 1;

        String index;
        String id;
        byte[] payload;

        @SneakyThrows
        public static Buffer of(String index, Object payload) {
            Buffer buffer = new Buffer();
            buffer.index = index;
            @SuppressWarnings("unchecked")
            Map<String, Object> data = payload instanceof Map
                ? ((Map) payload) :
                FastBeanCopier.copy(payload, HashMap::new);
            Object id = data.get("id");
            buffer.id = id == null ? null : String.valueOf(id);
            buffer.payload = ObjectMappers.JSON_MAPPER.writeValueAsBytes(data);
            return buffer;
        }

        void release() {

        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeUTF(index);
            SerializeUtils.writeNullableUTF(id, out);
            out.writeInt(payload.length);
            out.write(payload);
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
            index = in.readUTF();
            id = SerializeUtils.readNullableUTF(in);
            int length = in.readInt();
            payload = new byte[length];
            in.readFully(payload);
        }

        @Override
        public int usage() {
            return payload == null ? 64 : 64 + payload.length;
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

    protected Mono<Integer> doSave(Collection<Buffer> buffers) {
        int size = buffers.size();
        return Flux
            .fromIterable(buffers)
            .groupBy(Buffer::getIndex, Integer.MAX_VALUE)
            .flatMap(group -> {
                String index = group.key();
                return this
                    .getIndexForSave(index)
                    .flatMapMany(realIndex -> group
                        .map(buffer -> {
                            try {
                                IndexRequest request;
                                if (buffer.id != null) {
                                    request = new IndexRequest(realIndex).id(buffer.id);
                                } else {
                                    request = new IndexRequest(realIndex);
                                }
                                if (getRestClient().serverVersion().before(Version.V_7_0_0)) {
                                    request.type("_doc");
                                }
                                request.source(buffer.payload, XContentType.JSON);
                                return request;
                            } finally {
                                buffer.release();
                            }
                        }));
            })
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(lst -> {
                BulkRequest request = new BulkRequest();
                request.timeout(TimeValue.timeValueSeconds(9));
                if (buffer.refreshWhenWrite) {
                    request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                }
                lst.forEach(request::add);
                return restClient.bulk(request);
            })
            .doOnError((err) -> {
                //这里的错误都输出到控制台,输入到slf4j可能会造成日志递归.
                SystemUtils.printError("保存ElasticSearch数据失败:\n%s", () -> new Object[]{
                    org.hswebframework.utils.StringUtils.throwable2String(err)
                });
            })
            .map(response -> {
                if (response.hasFailures()) {
                    return 0;
                }
                return size;
            });
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

    private Mono<Long> doCount(SearchRequest request) {
        return restClient.count(request);
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
                                .indicesOptions(indexOptions));
    }

    protected Mono<QueryBuilder> createQueryBuilder(QueryParam queryParam, String index) {
        return indexManager
            .getIndexMetadata(index)
            .map(metadata -> QueryParamTranslator.createQueryBuilder(queryParam, metadata))
            .switchIfEmpty(Mono.fromSupplier(() -> QueryParamTranslator.createQueryBuilder(queryParam, null)));
    }

}
