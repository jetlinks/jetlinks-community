package org.jetlinks.community.elastic.search.service.reactive;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Generated;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.Version;
import org.elasticsearch.action.ActionRequest;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest;
import org.elasticsearch.action.admin.indices.close.CloseIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.flush.FlushRequest;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.admin.indices.open.OpenIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesRequest;
import org.elasticsearch.action.admin.indices.template.get.GetIndexTemplatesResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.main.MainRequest;
import org.elasticsearch.action.main.MainResponse;
import org.elasticsearch.action.search.*;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.GetAliasesResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.indices.GetFieldMappingsRequest;
import org.elasticsearch.client.indices.GetFieldMappingsResponse;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.IndexTemplatesExistRequest;
import org.elasticsearch.client.tasks.TaskSubmissionResponse;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.common.Priority;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.lucene.uid.Versions;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.ReindexRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.index.seqno.SequenceNumbers;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.mustache.SearchTemplateRequest;
import org.elasticsearch.script.mustache.SearchTemplateResponse;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.search.internal.SearchContext;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.tasks.TaskId;
import org.elasticsearch.xcontent.*;
import org.reactivestreams.Publisher;
import org.springframework.data.elasticsearch.client.ClientLogger;
import org.springframework.data.elasticsearch.client.ElasticsearchHost;
import org.springframework.data.elasticsearch.client.NoReachableHostException;
import org.springframework.data.elasticsearch.client.reactive.HostProvider;
import org.springframework.data.elasticsearch.client.reactive.RequestCreator;
import org.springframework.data.elasticsearch.client.util.NamedXContents;
import org.springframework.data.elasticsearch.client.util.RequestConverters;
import org.springframework.data.elasticsearch.client.util.ScrollState;
import org.springframework.data.elasticsearch.core.ResponseConverter;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.util.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.springframework.data.elasticsearch.client.util.RequestConverters.createContentType;

@Slf4j
@Generated
public class DefaultReactiveElasticsearchClient implements ReactiveElasticsearchClient,
    org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Cluster {
    private final HostProvider<?> hostProvider;
    private final RequestCreator requestCreator;
    private Supplier<HttpHeaders> headersSupplier = () -> HttpHeaders.EMPTY;

    /**
     * Create a new {@link org.springframework.data.elasticsearch.client.reactive.DefaultReactiveElasticsearchClient} using the given {@link HostProvider} to obtain server
     * connections and the given {@link RequestCreator}.
     *
     * @param hostProvider   must not be {@literal null}.
     * @param requestCreator must not be {@literal null}.
     */
    public DefaultReactiveElasticsearchClient(HostProvider hostProvider, RequestCreator requestCreator) {

        Assert.notNull(hostProvider, "HostProvider must not be null");
        Assert.notNull(requestCreator, "RequestCreator must not be null");

        this.hostProvider = hostProvider;
        this.requestCreator = requestCreator;
        version = info().block(Duration.ofSeconds(10)).getVersion();
    }

    public void setHeadersSupplier(Supplier<HttpHeaders> headersSupplier) {

        Assert.notNull(headersSupplier, "headersSupplier must not be null");

        this.headersSupplier = headersSupplier;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders)
     */
    @Override
    public Mono<Boolean> ping(HttpHeaders headers) {

        return sendRequest(new MainRequest(), requestCreator.ping(), RawActionResponse.class, headers)
            .flatMap(response -> response.releaseBody().thenReturn(response.statusCode().is2xxSuccessful()))
            .onErrorResume(NoReachableHostException.class, error -> Mono.just(false))
            .next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#info(org.springframework.http.HttpHeaders)
     */
    @Override
    public Mono<MainResponse> info(HttpHeaders headers) {

        return sendRequest(new MainRequest(), requestCreator.info(), MainResponse.class, headers) //
                                                                                                  .next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#get(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.GetRequest)
     */
    @Override
    public Mono<GetResult> get(HttpHeaders headers, GetRequest getRequest) {

        return sendRequest(getRequest, requestCreator.get(), GetResponse.class, headers) //
                                                                                         .filter(GetResponse::isExists) //
                                                                                         .map(DefaultReactiveElasticsearchClient::getResponseToGetResult) //
                                                                                         .next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#multiGet(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.MultiGetRequest)
     */
    @Override
    public Flux<MultiGetItemResponse> multiGet(HttpHeaders headers, MultiGetRequest multiGetRequest) {

        return sendRequest(multiGetRequest, requestCreator.multiGet(), MultiGetResponse.class, headers)
            .map(MultiGetResponse::getResponses) //
            .flatMap(Flux::fromArray); //
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#exists(org.springframework.http.HttpHeaders, org.elasticsearch.action.get.GetRequest)
     */
    @Override
    public Mono<Boolean> exists(HttpHeaders headers, GetRequest getRequest) {

        return sendRequest(getRequest, requestCreator.exists(), RawActionResponse.class, headers)
            .flatMap(response -> response.releaseBody().thenReturn(response
                                                                       .statusCode()
                                                                       .is2xxSuccessful()))
            .next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.index.IndexRequest)
     */
    @Override
    public Mono<IndexResponse> index(HttpHeaders headers, IndexRequest indexRequest) {
        return sendRequest(indexRequest, requestCreator.index(), IndexResponse.class, headers).publishNext();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#indices()
     */
    @Override
    public Indices indices() {
        return this;
    }

    @Override
    public Cluster cluster() {
        return this;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.update.UpdateRequest)
     */
    @Override
    public Mono<UpdateResponse> update(HttpHeaders headers, UpdateRequest updateRequest) {
        return sendRequest(updateRequest, requestCreator.update(), UpdateResponse.class, headers).publishNext();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.delete.DeleteRequest)
     */
    @Override
    public Mono<DeleteResponse> delete(HttpHeaders headers, DeleteRequest deleteRequest) {

        return sendRequest(deleteRequest, requestCreator.delete(), DeleteResponse.class, headers) //
                                                                                                  .publishNext();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#count(org.springframework.http.HttpHeaders, org.elasticsearch.action.search.SearchRequest)
     */
    @Override
    public Mono<Long> count(HttpHeaders headers, SearchRequest searchRequest) {
        searchRequest.source().trackTotalHits(true);
        searchRequest.source().size(0);
        searchRequest.source().fetchSource(false);
        return sendRequest(searchRequest, this::buildSearchRequest, SearchResponse.class, headers)
            .map(SearchResponse::getHits)
            .map(searchHits -> searchHits.getTotalHits().value)
            .next();
    }

    @Override
    public Flux<SearchHit> searchTemplate(HttpHeaders headers, SearchTemplateRequest searchTemplateRequest) {
        return sendRequest(searchTemplateRequest, requestCreator.searchTemplate(), SearchTemplateResponse.class, headers)
            .map(response -> response.getResponse().getHits()).flatMap(Flux::fromIterable);
    }

    protected Request buildSearchRequest(SearchRequest request) {
        //兼容6.x版本es
        if (version.before(Version.V_7_0_0) && request
            .source()
            .trackTotalHitsUpTo() != SearchContext.TRACK_TOTAL_HITS_DISABLED) {
            Request req = requestCreator.search().apply(request);
            JSONObject json = JSON.parseObject(requestBodyToString(req));
            json.put("track_total_hits", true);
            req.setJsonEntity(json.toJSONString());
            return req;
        }
        return requestCreator.search().apply(request);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.action.search.SearchRequest)
     */
    @Override
    public Flux<SearchHit> search(HttpHeaders headers, SearchRequest searchRequest) {

        return sendRequest(searchRequest, this::buildSearchRequest, SearchResponse.class, headers) //
                                                                                                   .map(SearchResponse::getHits) //
                                                                                                   .flatMap(Flux::fromIterable);
    }

    @Override
    public Mono<SearchResponse> searchForResponse(HttpHeaders headers, SearchRequest searchRequest) {
        return sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers).next();
    }

    @Override
    public Flux<Suggest> suggest(HttpHeaders headers, SearchRequest searchRequest) {
        return sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers) //
                                                                                                  .map(SearchResponse::getSuggest);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#aggregate(org.springframework.http.HttpHeaders, org.elasticsearch.action.search.SearchRequest)
     */
    @Override
    public Flux<Aggregation> aggregate(HttpHeaders headers, SearchRequest searchRequest) {

        Assert.notNull(headers, "headers must not be null");
        Assert.notNull(searchRequest, "searchRequest must not be null");

        searchRequest.source().size(0);
        searchRequest.source().trackTotalHits(false);

        return sendRequest(searchRequest, this::buildSearchRequest, SearchResponse.class, headers) //
                                                                                                   .map(SearchResponse::getAggregations) //
                                                                                                   .flatMap(Flux::fromIterable);
    }

    @Override@Nonnull
    public Flux<SearchHit> scroll(@Nonnull HttpHeaders headers, SearchRequest searchRequest) {

        TimeValue scrollTimeout = searchRequest.scroll() != null ? searchRequest.scroll().keepAlive()
            : TimeValue.timeValueMinutes(1);

        if (searchRequest.scroll() == null) {
            searchRequest.scroll(scrollTimeout);
        }

        return Flux
            .usingWhen(Mono.fromSupplier(ScrollState::new),
                       state -> this
                           .sendRequest(searchRequest, requestCreator.search(), SearchResponse.class, headers)
                           .expand(searchResponse -> {

                               state.updateScrollId(searchResponse.getScrollId());
                               if (isEmpty(searchResponse.getHits())) {
                                   return Mono.empty();
                               }

                               return this
                                   .sendRequest(new SearchScrollRequest(searchResponse.getScrollId()).scroll(scrollTimeout),
                                                requestCreator.scroll(),
                                                SearchResponse.class,
                                                headers);

                           }),
                       state -> cleanupScroll(headers, state),
                       (state, ex) -> cleanupScroll(headers, state),
                       state -> cleanupScroll(headers, state))
            .filter(it -> !isEmpty(it.getHits()))
            .map(SearchResponse::getHits)
            .flatMapIterable(Function.identity());
    }
    private static boolean isEmpty(@Nullable SearchHits hits) {
        return hits != null && hits.getHits() != null && hits.getHits().length == 0;
    }

    private Publisher<?> cleanupScroll(HttpHeaders headers, ScrollState state) {

        if (state.getScrollIds().isEmpty()) {
            return Mono.empty();
        }

        ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
        clearScrollRequest.scrollIds(state.getScrollIds());

        // just send the request, resources get cleaned up anyways after scrollTimeout has been reached.
        return sendRequest(clearScrollRequest, requestCreator.clearScroll(), ClearScrollResponse.class, headers);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.http.HttpHeaders, org.elasticsearch.index.reindex.DeleteByQueryRequest)
     */
    @Override
    public Mono<BulkByScrollResponse> deleteBy(HttpHeaders headers, DeleteByQueryRequest deleteRequest) {

        return sendRequest(deleteRequest, requestCreator.deleteByQuery(), BulkByScrollResponse.class, headers) //
                                                                                                               .publishNext();
    }

    @Override
    public Mono<ByQueryResponse> updateBy(HttpHeaders headers, UpdateByQueryRequest updateRequest) {
        return sendRequest(updateRequest, requestCreator.updateByQuery(), BulkByScrollResponse.class, headers)
            .next()
            .map(ResponseConverter::byQueryResponseOf);
    }

    static XContentType enforceSameContentType(IndexRequest indexRequest, @Nullable XContentType xContentType) {
        XContentType requestContentType = indexRequest.getContentType();
        if (requestContentType != XContentType.JSON && requestContentType != XContentType.SMILE) {
            throw new IllegalArgumentException("Unsupported content-type found for request with content-type ["
                                                   + requestContentType + "], only JSON and SMILE are supported");
        }
        if (xContentType == null) {
            return requestContentType;
        }
        if (requestContentType != xContentType) {
            throw new IllegalArgumentException("Mismatching content-type found for request with content-type ["
                                                   + requestContentType + "], previous requests have content-type [" + xContentType + ']');
        }
        return xContentType;
    }

    @SneakyThrows
    Request convertBulk(BulkRequest bulkRequest) {
        Request request = new Request(HttpMethod.POST.name(), "/_bulk");

        Params parameters = new Params(request);
        parameters.withTimeout(bulkRequest.timeout());
        parameters.withRefreshPolicy(bulkRequest.getRefreshPolicy());

        // parameters.withPipeline(bulkRequest.pipeline());
        // parameters.withRouting(bulkRequest.routing());

        // Bulk API only supports newline delimited JSON or Smile. Before executing
        // the bulk, we need to check that all requests have the same content-type
        // and this content-type is supported by the Bulk API.
        XContentType bulkContentType = null;
        for (int i = 0; i < bulkRequest.numberOfActions(); i++) {
            DocWriteRequest<?> action = bulkRequest.requests().get(i);

            DocWriteRequest.OpType opType = action.opType();
            if (opType == DocWriteRequest.OpType.INDEX || opType == DocWriteRequest.OpType.CREATE) {
                bulkContentType = enforceSameContentType((IndexRequest) action, bulkContentType);

            } else if (opType == DocWriteRequest.OpType.UPDATE) {
                UpdateRequest updateRequest = (UpdateRequest) action;
                if (updateRequest.doc() != null) {
                    bulkContentType = enforceSameContentType(updateRequest.doc(), bulkContentType);
                }
                if (updateRequest.upsertRequest() != null) {
                    bulkContentType = enforceSameContentType(updateRequest.upsertRequest(), bulkContentType);
                }
            }
        }

        if (bulkContentType == null) {
            bulkContentType = XContentType.JSON;
        }

        final byte separator = bulkContentType.xContent().streamSeparator();
        final ContentType requestContentType = createContentType(bulkContentType);

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (DocWriteRequest<?> action : bulkRequest.requests()) {
            DocWriteRequest.OpType opType = action.opType();

            try (XContentBuilder metadata = XContentBuilder.builder(bulkContentType.xContent())) {
                metadata.startObject();
                {
                    metadata.startObject(opType.getLowercase());
                    if (Strings.hasLength(action.index())) {
                        metadata.field("_index", action.index());
                    }
                    if (Strings.hasLength(action.type())) {
                        metadata.field("_type", action.type());
                    }
                    if (Strings.hasLength(action.id())) {
                        metadata.field("_id", action.id());
                    }
                    if (Strings.hasLength(action.routing())) {
                        metadata.field("routing", action.routing());
                    }
                    if (action.version() != Versions.MATCH_ANY) {
                        metadata.field("version", action.version());
                    }

                    VersionType versionType = action.versionType();
                    if (versionType != VersionType.INTERNAL) {
                        if (versionType == VersionType.EXTERNAL) {
                            metadata.field("version_type", "external");
                        } else if (versionType == VersionType.EXTERNAL_GTE) {
                            metadata.field("version_type", "external_gte");
                        }
                    }

                    if (action.ifSeqNo() != SequenceNumbers.UNASSIGNED_SEQ_NO) {
                        metadata.field("if_seq_no", action.ifSeqNo());
                        metadata.field("if_primary_term", action.ifPrimaryTerm());
                    }

                    if (opType == DocWriteRequest.OpType.INDEX || opType == DocWriteRequest.OpType.CREATE) {
                        IndexRequest indexRequest = (IndexRequest) action;
                        if (Strings.hasLength(indexRequest.getPipeline())) {
                            metadata.field("pipeline", indexRequest.getPipeline());
                        }
                    } else if (opType == DocWriteRequest.OpType.UPDATE) {
                        UpdateRequest updateRequest = (UpdateRequest) action;
                        if (updateRequest.retryOnConflict() > 0) {
                            metadata.field("retry_on_conflict", updateRequest.retryOnConflict());
                        }
                        if (updateRequest.fetchSource() != null) {
                            metadata.field("_source", updateRequest.fetchSource());
                        }
                    }
                    metadata.endObject();
                }
                metadata.endObject();

                BytesRef metadataSource = BytesReference.bytes(metadata).toBytesRef();
                content.write(metadataSource.bytes, metadataSource.offset, metadataSource.length);
                content.write(separator);
            }

            BytesRef source = null;
            if (opType == DocWriteRequest.OpType.INDEX || opType == DocWriteRequest.OpType.CREATE) {
                IndexRequest indexRequest = (IndexRequest) action;
                BytesReference indexSource = indexRequest.source();
                XContentType indexXContentType = indexRequest.getContentType();

                try (XContentParser parser = XContentHelper.createParser(
                    /*
                     * EMPTY and THROW are fine here because we just call
                     * copyCurrentStructure which doesn't touch the
                     * registry or deprecation.
                     */
                    NamedXContentRegistry.EMPTY, DeprecationHandler.THROW_UNSUPPORTED_OPERATION, indexSource,
                    indexXContentType)) {
                    try (XContentBuilder builder = XContentBuilder.builder(bulkContentType.xContent())) {
                        builder.copyCurrentStructure(parser);
                        source = BytesReference.bytes(builder).toBytesRef();
                    }
                }
            } else if (opType == DocWriteRequest.OpType.UPDATE) {
                source = XContentHelper.toXContent((UpdateRequest) action, bulkContentType, false).toBytesRef();
            }

            if (source != null) {
                content.write(source.bytes, source.offset, source.length);
                content.write(separator);
            }
        }
        request.setEntity(new ByteArrayEntity(content.toByteArray(), 0, content.size(), requestContentType));
        return request;
    }


    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#bulk(org.springframework.http.HttpHeaders, org.elasticsearch.action.bulk.BulkRequest)
     */
    @Override
    public Mono<BulkResponse> bulk(HttpHeaders headers, BulkRequest bulkRequest) {
        return sendRequest(bulkRequest, this::convertBulk, BulkResponse.class, headers) //
                                                                                        .publishNext();
    }

    @Override
    public Mono<BulkByScrollResponse> reindex(HttpHeaders headers, ReindexRequest reindexRequest) {
        return sendRequest(reindexRequest, requestCreator.reindex(), BulkByScrollResponse.class, headers).next();
    }

    @Override
    public Mono<String> submitReindex(HttpHeaders headers, ReindexRequest reindexRequest) {
        return sendRequest(reindexRequest, requestCreator.submitReindex(), TaskSubmissionResponse.class, headers).next()
                                                                                                                 .map(TaskSubmissionResponse::getTask);
    }

    // --> INDICES

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices#existsIndex(org.springframework.http.HttpHeaders, org.elasticsearch.action.admin.indices.get.GetIndexRequest)
     */
    @Override
    public Mono<Boolean> existsIndex(HttpHeaders headers, GetIndexRequest request) {
        return sendRequest(request, requestCreator.indexExists()
            , RawActionResponse.class, headers)
            .flatMap(response -> response
                .releaseBody()
                .thenReturn(response
                                .statusCode()
                                .is2xxSuccessful()))
            .onErrorReturn(false)
            .next();
    }

    @Override
    public Mono<Boolean> existsIndex(HttpHeaders headers, org.elasticsearch.client.indices.GetIndexRequest getIndexRequest) {
        return sendRequest(getIndexRequest, requestCreator.indexExistsRequest(), RawActionResponse.class, headers)
            .flatMap(response -> response
                .releaseBody()
                .thenReturn(response
                                .statusCode()
                                .is2xxSuccessful()))
            .next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices#deleteIndex(org.springframework.http.HttpHeaders, org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest)
     */
    @Override
    public Mono<Boolean> deleteIndex(HttpHeaders headers, DeleteIndexRequest request) {

        return sendRequest(request, requestCreator.indexDelete(), AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged)
            .next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices#createIndex(org.springframework.http.HttpHeaders, org.elasticsearch.action.admin.indices.create.CreateIndexRequest)
     */
    @Override
    public Mono<Boolean> createIndex(HttpHeaders headers, CreateIndexRequest createIndexRequest) {

        return sendRequest(createIndexRequest, requestCreator.indexCreate().andThen(request -> {
            request.addParameter("include_type_name", "true");
            return request;
        }), AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged)
            .next();
    }

    @Override
    public Mono<Boolean> createIndex(HttpHeaders headers, org.elasticsearch.client.indices.CreateIndexRequest createIndexRequest) {
        return sendRequest(createIndexRequest, requestCreator.createIndexRequest(), AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged)
            .next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices#openIndex(org.springframework.http.HttpHeaders, org.elasticsearch.action.admin.indices.open.OpenIndexRequest)
     */
    @Override
    public Mono<Void> openIndex(HttpHeaders headers, OpenIndexRequest request) {

        return sendRequest(request, requestCreator.indexOpen(), AcknowledgedResponse.class, headers)
            .then();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices#closeIndex(org.springframework.http.HttpHeaders, org.elasticsearch.action.admin.indices.close.CloseIndexRequest)
     */
    @Override
    public Mono<Void> closeIndex(HttpHeaders headers, CloseIndexRequest closeIndexRequest) {

        return sendRequest(closeIndexRequest, requestCreator.indexClose(), AcknowledgedResponse.class, headers) //
                                                                                                                .then();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices#refreshIndex(org.springframework.http.HttpHeaders, org.elasticsearch.action.admin.indices.refresh.RefreshRequest)
     */
    @Override
    public Mono<Void> refreshIndex(HttpHeaders headers, RefreshRequest refreshRequest) {

        return sendRequest(refreshRequest, requestCreator.indexRefresh(), RefreshResponse.class, headers) //
                                                                                                          .then();
    }

    @Override
    public Mono<Boolean> putMapping(HttpHeaders headers, PutMappingRequest putMappingRequest) {
        return sendRequest(putMappingRequest, requestCreator.putMapping(), AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged)
            .next();
    }

    @Override
    public Mono<Boolean> putMapping(HttpHeaders headers, org.elasticsearch.client.indices.PutMappingRequest putMappingRequest) {

        return sendRequest(putMappingRequest, this::createPutMapping, AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged)
            .next();
    }

    private Request createPutMapping(org.elasticsearch.client.indices.PutMappingRequest putMappingRequest) {
        Request request = requestCreator.putMappingRequest().apply(putMappingRequest);
        Request newReq = new Request(request.getMethod(), request.getEndpoint());

        Params params = new Params(newReq)
            .withTimeout(putMappingRequest.timeout())
            .withMasterTimeout(putMappingRequest.masterNodeTimeout());
        if (serverVersion().before(Version.V_7_0_0)) {
            params.putParam("include_type_name", "false");
        }
        newReq.setEntity(request.getEntity());
        return newReq;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Indices#flushIndex(org.springframework.http.HttpHeaders, org.elasticsearch.action.admin.indices.flush.FlushRequest)
     */
    @Override
    public Mono<Void> flushIndex(HttpHeaders headers, FlushRequest flushRequest) {

        return sendRequest(flushRequest, requestCreator.flushIndex(), FlushResponse.class, headers) //
                                                                                                    .then();
    }

    @Override
    public Mono<GetSettingsResponse> getSettings(HttpHeaders headers, GetSettingsRequest getSettingsRequest) {
        return sendRequest(getSettingsRequest, requestCreator.getSettings(), GetSettingsResponse.class, headers).next();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient#ping(org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.ReactiveElasticsearchClientCallback)
     */
    @Override
    public <T> Mono<T> execute(ReactiveElasticsearchClientCallback<T> callback) {

        return this.hostProvider
            .getActive(HostProvider.Verification.LAZY) //
            .flatMap(callback::doWithClient) //
            .onErrorResume(throwable -> {

                if (isCausedByConnectionException(throwable)) {
                    return hostProvider.getActive(HostProvider.Verification.ACTIVE) //
                                       .flatMap(callback::doWithClient);
                }

                return Mono.error(throwable);
            });
    }

    private boolean isCausedByConnectionException(Throwable throwable) {

        Throwable t = throwable;
        do {

            if (t instanceof ConnectException) {
                return true;
            }

            t = t.getCause();
        } while (t != null);

        return false;
    }

    @Override
    public Mono<Status> status() {

        return hostProvider.clusterInfo() //
                           .map(it -> new ClientStatus(it.getNodes()));
    }

    // --> Private Response helpers

    private static GetResult getResponseToGetResult(GetResponse response) {

        return new GetResult(response.getIndex(), response.getType(), response.getId(), response.getSeqNo(),
                             response.getPrimaryTerm(), response.getVersion(), response.isExists(), response.getSourceAsBytesRef(),
                             response.getFields(), null);
    }

    // -->

    private <REQ, RESP> Flux<RESP> sendRequest(REQ request, Function<REQ, Request> converter, Class<RESP> responseType,
                                               HttpHeaders headers) {
        return sendRequest(converter.apply(request), responseType, headers);
    }

    private <Resp> Flux<Resp> sendRequest(Request request, Class<Resp> responseType, HttpHeaders headers) {

        String logId = ClientLogger.newLogId();

        return Flux
            .from(execute(webClient -> sendRequest(webClient, logId, request, headers).exchangeToMono(clientResponse -> {
                Publisher<? extends Resp> publisher = readResponseBody(logId, request, clientResponse, responseType);
                return Mono.from(publisher);
            })));
    }

    private <Req extends ActionRequest, Resp> Flux<Resp> sendRequest(Req request,
                                                                     Function<Req, Request> converter,
                                                                     Class<Resp> responseType,
                                                                     HttpHeaders headers) {
        return sendRequest(request, converter, responseType, headers, DefaultReactiveElasticsearchClient::doDecode);
    }

    private <Req extends ActionRequest, Resp> Flux<Resp> sendRequest(Req request,
                                                                     Function<Req, Request> converter,
                                                                     Class<Resp> responseType,
                                                                     HttpHeaders headers,
                                                                     Function3<ClientResponse, Class<Resp>, String, Mono<Resp>> decoder) {
        return sendRequest(converter.apply(request), responseType, headers, decoder);
    }

    private <Resp> Flux<Resp> sendRequest(Request request,
                                          Class<Resp> responseType,
                                          HttpHeaders headers,
                                          Function3<ClientResponse, Class<Resp>, String, Mono<Resp>> decoder) {

        String logId = ClientLogger.newLogId();

        return execute(webClient -> Mono.just(this.sendRequest(webClient, logId, request, headers)))
            .flatMapMany(spec -> {
                return spec.exchangeToFlux(response -> {
                    return Flux.from(
                                   this.readResponseBody(logId, request, response, responseType, decoder)
                               )
                               .cast(responseType);
                });
            });
    }

    private WebClient.RequestBodySpec sendRequest(WebClient webClient, String logId, Request request, HttpHeaders headers) {

        WebClient.RequestBodySpec requestBodySpec = webClient.method(HttpMethod.valueOf(request
                                                                                            .getMethod()
                                                                                            .toUpperCase())) //
                                                             .uri(builder -> {

                                                                 builder = builder.path(request.getEndpoint());

                                                                 if (!ObjectUtils.isEmpty(request.getParameters())) {
                                                                     for (Map.Entry<String, String> entry : request
                                                                         .getParameters()
                                                                         .entrySet()) {
                                                                         builder = builder.queryParam(entry.getKey(), entry.getValue());
                                                                     }
                                                                 }
                                                                 return builder.build();
                                                             }) //
                                                             .attribute(ClientRequest.LOG_ID_ATTRIBUTE, logId) //
                                                             .headers(theHeaders -> {

                                                                 // add all the headers explicitly set
                                                                 theHeaders.addAll(headers);

                                                                 // and now those that might be set on the request.
                                                                 if (request.getOptions() != null) {

                                                                     if (!ObjectUtils.isEmpty(request
                                                                                                  .getOptions()
                                                                                                  .getHeaders())) {
                                                                         request
                                                                             .getOptions()
                                                                             .getHeaders()
                                                                             .forEach(it -> theHeaders.add(it.getName(), it.getValue()));
                                                                     }
                                                                 }
                                                             });

        if (request.getEntity() != null) {

            Lazy<String> body = bodyExtractor(request);

            ClientLogger.logRequest(logId, request
                                        .getMethod()
                                        .toUpperCase(), request.getEndpoint(), request.getParameters(),
                                    body::get);

            requestBodySpec.contentType(MediaType.valueOf(request.getEntity().getContentType().getValue()))
                           .body(Mono.fromSupplier(body), String.class);
        } else {
            ClientLogger.logRequest(logId, request
                .getMethod()
                .toUpperCase(), request.getEndpoint(), request.getParameters());
        }

        return requestBodySpec;
    }

    private Lazy<String> bodyExtractor(Request request) {

        return Lazy.of(() -> requestBodyToString(request));
    }

    @SneakyThrows
    private String requestBodyToString(Request request) {
        return EntityUtils.toString(request.getEntity());
    }

    private <T> Publisher<? extends T> readResponseBody(String logId, Request request, ClientResponse response,
                                                        Class<T> responseType) {
        return readResponseBody(logId, request, response, responseType, DefaultReactiveElasticsearchClient::doDecode);
    }

    private <T> Publisher<? extends T> readResponseBody(String logId,
                                                        Request request,
                                                        ClientResponse response,
                                                        Class<T> responseType,
                                                        Function3<ClientResponse, Class<T>, String, Mono<T>> decoder) {

        if (RawActionResponse.class.equals(responseType)) {

            ClientLogger.logRawResponse(logId, response.statusCode());
            return Mono.just(responseType.cast(RawActionResponse.create(response)));
        }

        if (response.statusCode().is5xxServerError()) {

            ClientLogger.logRawResponse(logId, response.statusCode());
            return handleServerError(request, response);
        }

        if (response.statusCode().is4xxClientError()) {

            ClientLogger.logRawResponse(logId, response.statusCode());
            return handleClientError(logId, request, response, responseType);
        }

        return response.body(BodyExtractors.toMono(byte[].class)) //
                       .map(it -> new String(it, StandardCharsets.UTF_8)) //
                       .doOnNext(it -> ClientLogger.logResponse(logId, response.statusCode(), it)) //
                       .flatMap(content -> decoder.apply(response, responseType, content));
    }


    private static <T> Mono<T> doDecode(ClientResponse response, Class<T> responseType, String content) {

        String mediaType = response
            .headers()
            .contentType()
            .map(MediaType::toString)
            .orElse(XContentType.JSON.mediaType());

        try {

            Method fromXContent = ReflectionUtils.findMethod(responseType, "fromXContent", XContentParser.class);
            if (fromXContent == null) {
                fromXContent = ReflectionUtils.findMethod(responseType, "fromXContext", XContentParser.class);
            }
            return Mono.justOrEmpty(responseType
                                        .cast(ReflectionUtils.invokeMethod(fromXContent, responseType, createParser(mediaType, content))));

        } catch (Throwable errorParseFailure) { // cause elasticsearch also uses AssertionError

            try {
                return Mono.error(BytesRestResponse.errorFromXContent(createParser(mediaType, content)));
            } catch (Exception e) {
                return Mono
                    .error(new ElasticsearchStatusException(content,
                                                            RestStatus.fromCode(response.statusCode().value()),
                                                            errorParseFailure));
            }
        }
    }

    private static XContentParser createParser(String mediaType, String content) throws IOException {
        XContentType type = XContentType.fromMediaTypeOrFormat(mediaType);
        if (type == null) {
            throw new IOException(content);
        }
        return XContentType.fromMediaTypeOrFormat(mediaType) //
                           .xContent() //
                           .createParser(new NamedXContentRegistry(NamedXContents.getDefaultNamedXContents()),
                                         DeprecationHandler.THROW_UNSUPPORTED_OPERATION, content);
    }

    private <T> Publisher<? extends T> handleServerError(Request request, ClientResponse response) {

        int statusCode = response.statusCode().value();
        RestStatus status = RestStatus.fromCode(statusCode);
        String mediaType = response
            .headers()
            .contentType()
            .map(MediaType::toString)
            .orElse(XContentType.JSON.mediaType());

        return response
            .body(BodyExtractors.toMono(byte[].class)) //
            .switchIfEmpty(Mono.error(
                () -> new ElasticsearchStatusException(String.format("%s request to %s returned error code %s and no body.",
                                                                     request.getMethod(),
                                                                     request.getEndpoint(),
                                                                     statusCode),
                                                       status)))
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8)) //
            .flatMap(content -> contentOrError(content, mediaType, status))
            .flatMap(unused -> Mono
                .error(() -> new ElasticsearchStatusException(String.format("%s request to %s returned error code %s.",
                                                                            request.getMethod(),
                                                                            request.getEndpoint(),
                                                                            statusCode),
                                                              status)));
    }

    private <T> Publisher<? extends T> handleClientError(String logId, Request request, ClientResponse response,
                                                         Class<T> responseType) {

        int statusCode = response.statusCode().value();
        RestStatus status = RestStatus.fromCode(statusCode);
        String mediaType = response
            .headers()
            .contentType()
            .map(MediaType::toString)
            .orElse(XContentType.JSON.mediaType());

        return response.body(BodyExtractors.toMono(byte[].class)) //
                       .map(bytes -> new String(bytes, StandardCharsets.UTF_8)) //
                       .flatMap(content -> contentOrError(content, mediaType, status)) //
                       .doOnNext(content -> ClientLogger.logResponse(logId, response.statusCode(), content)) //
                       .flatMap(content -> doDecode(response, responseType, content));
    }

    // region ElasticsearchException helper

    /**
     * checks if the given content body contains an {@link ElasticsearchException}, if yes it is returned in a Mono.error.
     * Otherwise the content is returned in the Mono
     *
     * @param content   the content to analyze
     * @param mediaType the returned media type
     * @param status    the response status
     * @return a Mono with the content or an Mono.error
     */
    private static Mono<String> contentOrError(String content, String mediaType, RestStatus status) {

        ElasticsearchException exception = getElasticsearchException(content, mediaType, status);

        if (exception != null) {
            if (status == RestStatus.NOT_FOUND) {
                log.warn(exception.getMessage(), exception);
                return Mono.empty();
            }
            StringBuilder sb = new StringBuilder();
            buildExceptionMessages(sb, exception);
            return Mono.error(new ElasticsearchStatusException(sb.toString(), status, exception));
        }

        return Mono.just(content);
    }

    /**
     * tries to parse an {@link ElasticsearchException} from the given body content
     *
     * @param content   the content to analyse
     * @param mediaType the type of the body content
     * @return an {@link ElasticsearchException} or {@literal null}.
     */
    @Nullable
    private static ElasticsearchException getElasticsearchException(String content, String mediaType, RestStatus status) {

        try {
            XContentParser parser = createParser(mediaType, content);
            // we have a JSON object with an error and a status field
            XContentParser.Token token = parser.nextToken(); // Skip START_OBJECT

            do {
                token = parser.nextToken();

                if ("error".equals(parser.currentName())) {
                    return ElasticsearchException.failureFromXContent(parser);
                }
            } while (token == XContentParser.Token.FIELD_NAME);

            return null;
        } catch (IOException e) {
            return new ElasticsearchStatusException(content, status);
        }
    }

    private static void buildExceptionMessages(StringBuilder sb, Throwable t) {

        sb.append(t.getMessage());
        for (Throwable throwable : t.getSuppressed()) {
            sb.append(", ");
            buildExceptionMessages(sb, throwable);
        }
    }

    @Override
    public Mono<SearchResponse> searchForPage(SearchRequest request) {
        // if (version.after(Version.V_7_0_0)) {
        request.source().trackTotalHits(true);
        // }
        return this
            .sendRequest(request, this::buildSearchRequest, SearchResponse.class, HttpHeaders.EMPTY)
            .singleOrEmpty()
            .doOnSuccess(res -> log
                .trace("execute search {} {} : {}", request.indices(), res.getTook(), request.source()))
            .doOnError(err -> log.warn("execute search {} error : {}", request.indices(), request.source(), err));
    }

    @SneakyThrows
    protected Request convertMultiSearchRequest(MultiSearchRequest searchRequest) {
        Request request = RequestConverters.multiSearch(searchRequest);
        if (log.isTraceEnabled()) {
            log.trace("execute elasticsearch multi search: {}", requestBodyToString(request));
        }
        return request;
    }

    @Override
    @SneakyThrows
    public Mono<MultiSearchResponse> multiSearch(MultiSearchRequest request) {
        Function3<ClientResponse, Class<MultiSearchResponse>, String, Mono<MultiSearchResponse>> decoder;
        if (version.before(Version.V_7_0_0)) {
            //适配6.x响应格式
            decoder = (clientResponse, multiSearchResponseClass, s) -> {
                JSONObject data = JSON.parseObject(s);
                int took = data.getJSONArray("responses")
                               .stream()
                               .map(JSONObject.class::cast)
                               .map(json -> json.getIntValue("took"))
                               .reduce(Math::addExact)
                               .orElse(0);
                data.put("took", took);
                return DefaultReactiveElasticsearchClient.doDecode(clientResponse, multiSearchResponseClass, data.toJSONString());
            };
        } else {
            decoder = DefaultReactiveElasticsearchClient::doDecode;
        }
        return sendRequest(request,
                           this::convertMultiSearchRequest,
                           MultiSearchResponse.class,
                           HttpHeaders.EMPTY,
                           decoder)
            .singleOrEmpty();
    }

    Request convertGetMappingRequest(GetMappingsRequest getMappingsRequest) {
        String[] indices = getMappingsRequest.indices() == null ? Strings.EMPTY_ARRAY : getMappingsRequest.indices();

        Request request = new Request(HttpGet.METHOD_NAME, "/" + String.join(",", indices) + "/_mapping");

        Params parameters = new Params(request);
        parameters.withMasterTimeout(getMappingsRequest.masterNodeTimeout());
        parameters.withIndicesOptions(getMappingsRequest.indicesOptions());
        parameters.withLocal(getMappingsRequest.local());
        parameters.putParam("include_type_name", "true");
        return request;
    }


    @Override
    public Mono<GetMappingsResponse> getMapping(GetMappingsRequest request) {

        return sendRequest(request, this::convertGetMappingRequest, GetMappingsResponse.class, HttpHeaders.EMPTY)
            .singleOrEmpty();
    }

    @Override
    public Mono<GetMappingsResponse> getMapping(HttpHeaders headers, GetMappingsRequest getMappingsRequest) {
        return sendRequest(getMappingsRequest, requestCreator.getMapping(),
                           GetMappingsResponse.class, headers).next();
    }

    @Override
    public Mono<org.elasticsearch.client.indices.GetMappingsResponse> getMapping(HttpHeaders headers, org.elasticsearch.client.indices.GetMappingsRequest getMappingsRequest) {
        return sendRequest(getMappingsRequest, requestCreator.getMappingRequest(), org.elasticsearch.client.indices.GetMappingsResponse.class, headers) //
                                                                                                                                                        .next();
    }

    @Override
    public Mono<GetFieldMappingsResponse> getFieldMapping(HttpHeaders headers,
                                                          GetFieldMappingsRequest getFieldMappingsRequest) {
        return sendRequest(getFieldMappingsRequest, requestCreator.getFieldMapping(), GetFieldMappingsResponse.class,
                           headers).next();
    }

    @Override
    public Mono<Boolean> updateAliases(HttpHeaders headers, IndicesAliasesRequest indicesAliasesRequest) {
        return sendRequest(indicesAliasesRequest, requestCreator.updateAlias(), AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged).next();
    }

    @Override
    public Mono<GetAliasesResponse> getAliases(HttpHeaders headers, GetAliasesRequest getAliasesRequest) {
        return sendRequest(getAliasesRequest, requestCreator.getAlias(), GetAliasesResponse.class, headers).next();
    }

    @Override
    public Mono<Boolean> putTemplate(HttpHeaders headers, org.elasticsearch.client.indices.PutIndexTemplateRequest putIndexTemplateRequest) {
        return sendRequest(putIndexTemplateRequest, requestCreator.putTemplate(), AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged).next();
    }

    @Override
    public Mono<org.elasticsearch.client.indices.GetIndexTemplatesResponse> getTemplate(HttpHeaders headers,
                                                                                        org.elasticsearch.client.indices.GetIndexTemplatesRequest getIndexTemplatesRequest) {
        return (sendRequest(getIndexTemplatesRequest, requestCreator.getTemplates(), org.elasticsearch.client.indices.GetIndexTemplatesResponse.class,
                            headers)).next();
    }

    @Override
    public Mono<Boolean> existsTemplate(HttpHeaders headers, IndexTemplatesExistRequest indexTemplatesExistRequest) {
        return sendRequest(indexTemplatesExistRequest, requestCreator.templatesExist(),
                           RawActionResponse.class, headers)
            .flatMap(response -> response
                .releaseBody()
                .thenReturn(response.statusCode().is2xxSuccessful()))
            .next();
    }

    @Override
    public Mono<Boolean> deleteTemplate(HttpHeaders headers, DeleteIndexTemplateRequest deleteIndexTemplateRequest) {
        return sendRequest(deleteIndexTemplateRequest, requestCreator.deleteTemplate(), AcknowledgedResponse.class, headers)
            .map(AcknowledgedResponse::isAcknowledged).next();
    }

    @Override
    public Mono<GetIndexResponse> getIndex(HttpHeaders headers, org.elasticsearch.client.indices.GetIndexRequest getIndexRequest) {
        return sendRequest(getIndexRequest, requestCreator.getIndex(), GetIndexResponse.class, headers).next();
    }

    Request convertGetIndexTemplateRequest(GetIndexTemplatesRequest getIndexTemplatesRequest) {
        Request request = new Request(HttpGet.METHOD_NAME, "/_template/" + String.join(",", getIndexTemplatesRequest.names()));
        Params params = new Params(request);
        params.putParam("include_type_name", "true");
        return request;
    }

    @Override
    public Mono<GetIndexTemplatesResponse> getTemplate(GetIndexTemplatesRequest request) {
        return sendRequest(request, this::convertGetIndexTemplateRequest, GetIndexTemplatesResponse.class, HttpHeaders.EMPTY)
            .singleOrEmpty();
    }

    @SneakyThrows
    Request convertPutIndexTemplateRequest(PutIndexTemplateRequest putIndexTemplateRequest) {
        Request request = new Request(HttpPut.METHOD_NAME, "/_template/" + putIndexTemplateRequest.name());
        Params params = new Params(request);
        params.withMasterTimeout(putIndexTemplateRequest.masterNodeTimeout());
        if (putIndexTemplateRequest.create()) {
            params.putParam("create", Boolean.TRUE.toString());
        }
        if (Strings.hasText(putIndexTemplateRequest.cause())) {
            params.putParam("cause", putIndexTemplateRequest.cause());
        }
        params.putParam("include_type_name", "true");
        BytesRef source = XContentHelper.toXContent(putIndexTemplateRequest, XContentType.JSON, false).toBytesRef();
        request.setEntity(new ByteArrayEntity(source.bytes, source.offset, source.length, ContentType.APPLICATION_JSON));
        return request;
    }

    @Override
    public Mono<AcknowledgedResponse> updateTemplate(PutIndexTemplateRequest request) {
        return sendRequest(request, this::convertPutIndexTemplateRequest, AcknowledgedResponse.class, HttpHeaders.EMPTY)
            .singleOrEmpty();
    }

    private Version version = Version.CURRENT;

    @Override
    public Version serverVersion() {
        return version;
    }

    @Override
    public Mono<ClusterHealthResponse> health(HttpHeaders headers, ClusterHealthRequest clusterHealthRequest) {
        return sendRequest(clusterHealthRequest, requestCreator.clusterHealth(), ClusterHealthResponse.class, headers)
            .next();
    }

    // endregion

    // region internal classes

    /**
     * Reactive client {@link Status} implementation.
     *
     * @author Christoph Strobl
     */
    @Generated
    static class ClientStatus implements Status {

        private final Collection<ElasticsearchHost> connectedHosts;

        ClientStatus(Collection<ElasticsearchHost> connectedHosts) {
            this.connectedHosts = connectedHosts;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient.Status#hosts()
         */
        @Override
        public Collection<ElasticsearchHost> hosts() {
            return connectedHosts;
        }
    }

    @Generated
    static class Params {
        private final Request request;

        Params(Request request) {
            this.request = request;
        }

        Params putParam(String name, String value) {
            if (Strings.hasLength(value)) {
                request.addParameter(name, value);
            }
            return this;
        }

        Params putParam(String key, TimeValue value) {
            if (value != null) {
                return putParam(key, value.getStringRep());
            }
            return this;
        }

        Params withDocAsUpsert(boolean docAsUpsert) {
            if (docAsUpsert) {
                return putParam("doc_as_upsert", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withFetchSourceContext(FetchSourceContext fetchSourceContext) {
            if (fetchSourceContext != null) {
                if (!fetchSourceContext.fetchSource()) {
                    putParam("_source", Boolean.FALSE.toString());
                }
                if (fetchSourceContext.includes() != null && fetchSourceContext.includes().length > 0) {
                    putParam("_source_includes", String.join(",", fetchSourceContext.includes()));
                }
                if (fetchSourceContext.excludes() != null && fetchSourceContext.excludes().length > 0) {
                    putParam("_source_excludes", String.join(",", fetchSourceContext.excludes()));
                }
            }
            return this;
        }

        Params withFields(String[] fields) {
            if (fields != null && fields.length > 0) {
                return putParam("fields", String.join(",", fields));
            }
            return this;
        }

        Params withMasterTimeout(TimeValue masterTimeout) {
            return putParam("master_timeout", masterTimeout);
        }

        Params withPipeline(String pipeline) {
            return putParam("pipeline", pipeline);
        }

        Params withPreference(String preference) {
            return putParam("preference", preference);
        }

        Params withRealtime(boolean realtime) {
            if (!realtime) {
                return putParam("realtime", Boolean.FALSE.toString());
            }
            return this;
        }

        Params withRefresh(boolean refresh) {
            if (refresh) {
                return withRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            }
            return this;
        }

        Params withRefreshPolicy(WriteRequest.RefreshPolicy refreshPolicy) {
            if (refreshPolicy != WriteRequest.RefreshPolicy.NONE) {
                return putParam("refresh", refreshPolicy.getValue());
            }
            return this;
        }

        Params withRequestsPerSecond(float requestsPerSecond) {
            // the default in AbstractBulkByScrollRequest is Float.POSITIVE_INFINITY,
            // but we don't want to add that to the URL parameters, instead we use -1
            if (Float.isFinite(requestsPerSecond)) {
                return putParam("requests_per_second", Float.toString(requestsPerSecond));
            } else {
                return putParam("requests_per_second", "-1");
            }
        }

        Params withRetryOnConflict(int retryOnConflict) {
            if (retryOnConflict > 0) {
                return putParam("retry_on_conflict", String.valueOf(retryOnConflict));
            }
            return this;
        }

        Params withRouting(String routing) {
            return putParam("routing", routing);
        }

        Params withStoredFields(String[] storedFields) {
            if (storedFields != null && storedFields.length > 0) {
                return putParam("stored_fields", String.join(",", storedFields));
            }
            return this;
        }

        Params withTimeout(TimeValue timeout) {
            return putParam("timeout", timeout);
        }

        Params withVersion(long version) {
            if (version != Versions.MATCH_ANY) {
                return putParam("version", Long.toString(version));
            }
            return this;
        }

        Params withVersionType(VersionType versionType) {
            if (versionType != VersionType.INTERNAL) {
                return putParam("version_type", versionType.name().toLowerCase(Locale.ROOT));
            }
            return this;
        }

        Params withIfSeqNo(long seqNo) {
            if (seqNo != SequenceNumbers.UNASSIGNED_SEQ_NO) {
                return putParam("if_seq_no", Long.toString(seqNo));
            }
            return this;
        }

        Params withIfPrimaryTerm(long primaryTerm) {
            if (primaryTerm != SequenceNumbers.UNASSIGNED_PRIMARY_TERM) {
                return putParam("if_primary_term", Long.toString(primaryTerm));
            }
            return this;
        }

        Params withWaitForActiveShards(ActiveShardCount activeShardCount) {
            return withWaitForActiveShards(activeShardCount, ActiveShardCount.DEFAULT);
        }

        Params withWaitForActiveShards(ActiveShardCount activeShardCount, ActiveShardCount defaultActiveShardCount) {
            if (activeShardCount != null && activeShardCount != defaultActiveShardCount) {
                // in Elasticsearch 7, "default" cannot be sent anymore, so it needs to be mapped to the default value of 1
                String value = activeShardCount == ActiveShardCount.DEFAULT ? "1"
                    : activeShardCount.toString().toLowerCase(Locale.ROOT);
                return putParam("wait_for_active_shards", value);
            }
            return this;
        }

        Params withIndicesOptions(IndicesOptions indicesOptions) {
            withIgnoreUnavailable(indicesOptions.ignoreUnavailable());
            putParam("allow_no_indices", Boolean.toString(indicesOptions.allowNoIndices()));
            String expandWildcards;
            if (!indicesOptions.expandWildcardsOpen() && !indicesOptions.expandWildcardsClosed()) {
                expandWildcards = "none";
            } else {
                StringJoiner joiner = new StringJoiner(",");
                if (indicesOptions.expandWildcardsOpen()) {
                    joiner.add("open");
                }
                if (indicesOptions.expandWildcardsClosed()) {
                    joiner.add("closed");
                }
                expandWildcards = joiner.toString();
            }
            putParam("expand_wildcards", expandWildcards);
            return this;
        }

        Params withIgnoreUnavailable(boolean ignoreUnavailable) {
            // Always explicitly place the ignore_unavailable value.
            putParam("ignore_unavailable", Boolean.toString(ignoreUnavailable));
            return this;
        }

        Params withHuman(boolean human) {
            if (human) {
                putParam("human", "true");
            }
            return this;
        }

        Params withLocal(boolean local) {
            if (local) {
                putParam("local", "true");
            }
            return this;
        }

        Params withIncludeDefaults(boolean includeDefaults) {
            if (includeDefaults) {
                return putParam("include_defaults", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withPreserveExisting(boolean preserveExisting) {
            if (preserveExisting) {
                return putParam("preserve_existing", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withDetailed(boolean detailed) {
            if (detailed) {
                return putParam("detailed", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withWaitForCompletion(Boolean waitForCompletion) {
            return putParam("wait_for_completion", waitForCompletion.toString());
        }

        Params withNodes(String[] nodes) {
            if (nodes != null && nodes.length > 0) {
                return putParam("nodes", String.join(",", nodes));
            }
            return this;
        }

        Params withActions(String[] actions) {
            if (actions != null && actions.length > 0) {
                return putParam("actions", String.join(",", actions));
            }
            return this;
        }

        Params withTaskId(TaskId taskId) {
            if (taskId != null && taskId.isSet()) {
                return putParam("task_id", taskId.toString());
            }
            return this;
        }

        Params withParentTaskId(TaskId parentTaskId) {
            if (parentTaskId != null && parentTaskId.isSet()) {
                return putParam("parent_task_id", parentTaskId.toString());
            }
            return this;
        }

        Params withVerify(boolean verify) {
            if (verify) {
                return putParam("verify", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withWaitForStatus(ClusterHealthStatus status) {
            if (status != null) {
                return putParam("wait_for_status", status.name().toLowerCase(Locale.ROOT));
            }
            return this;
        }

        Params withWaitForNoRelocatingShards(boolean waitNoRelocatingShards) {
            if (waitNoRelocatingShards) {
                return putParam("wait_for_no_relocating_shards", Boolean.TRUE.toString());
            }
            return this;
        }


        Params withWaitForNoInitializingShards(boolean waitNoInitShards) {
            if (waitNoInitShards) {
                return putParam("wait_for_no_initializing_shards", Boolean.TRUE.toString());
            }
            return this;
        }

        Params withWaitForNodes(String waitForNodes) {
            return putParam("wait_for_nodes", waitForNodes);
        }

        Params withLevel(ClusterHealthRequest.Level level) {
            return putParam("level", level.name().toLowerCase(Locale.ROOT));
        }

        Params withWaitForEvents(Priority waitForEvents) {
            if (waitForEvents != null) {
                return putParam("wait_for_events", waitForEvents.name().toLowerCase(Locale.ROOT));
            }
            return this;
        }

    }

}
