package org.jetlinks.community.elastic.search.index;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.enums.FieldType;
import org.jetlinks.community.elastic.search.index.mapping.IndexMappingMetadata;
import org.jetlinks.community.elastic.search.index.mapping.IndicesMappingCenter;
import org.jetlinks.community.elastic.search.index.mapping.SingleMappingMetadata;
import org.jetlinks.community.elastic.search.service.IndexOperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Service
@Slf4j
public class DefaultIndexOperationService implements IndexOperationService {

    private final ElasticRestClient restClient;

    private final IndicesMappingCenter indicesMappingCenter;

    @Autowired
    public DefaultIndexOperationService(ElasticRestClient restClient, IndicesMappingCenter indicesMappingCenter) {
        this.restClient = restClient;
        this.indicesMappingCenter = indicesMappingCenter;
    }


    @Override
    public Mono<Boolean> indexIsExists(String index) {
        return Mono.create(sink -> {
            try {
                GetIndexRequest request = new GetIndexRequest(index);
                sink.success(restClient.getQueryClient().indices().exists(request, RequestOptions.DEFAULT));
            } catch (Exception e) {
                log.error("查询es index 是否存在失败", e);
                sink.error(e);
            }
        });
    }

    @Override
    public Mono<Boolean> init(CreateIndexRequest request) {
        return indexIsExists(request.index())
                .filter(bool -> !bool)
                .flatMap(b -> Mono.create(sink -> {
                    restClient.getQueryClient().indices().createAsync(request, RequestOptions.DEFAULT, new ActionListener<CreateIndexResponse>() {
                        @Override
                        public void onResponse(CreateIndexResponse createIndexResponse) {
                            sink.success(createIndexResponse.isAcknowledged());
                        }

                        @Override
                        public void onFailure(Exception e) {
                            sink.error(e);
                        }
                    });
                }));

    }


    @Override
    public Mono<IndexMappingMetadata> getIndexMappingMetadata(String index) {
        return indicesMappingCenter.getIndexMappingMetaData(index)
                .map(Mono::just).orElseGet(() ->
                        getIndexMapping(index)
                                .doOnNext(indicesMappingCenter::register));
    }

    private Mono<IndexMappingMetadata> getIndexMapping(String index) {
        return indexIsExists(index)
                .filter(Boolean::booleanValue)
                .flatMap(bool -> Mono.create(sink -> {
                    if (bool) {
                        GetMappingsRequest mappingsRequest = new GetMappingsRequest();
                        mappingsRequest.indices(index);
                        restClient.getQueryClient().indices().getMappingAsync(mappingsRequest, RequestOptions.DEFAULT, new ActionListener<GetMappingsResponse>() {
                            @Override
                            public void onResponse(GetMappingsResponse getMappingsResponse) {
                                //index存在时 getMappingsResponse.mappings().get(index).getSourceAsMap().get("properties") 不会为空
                                sink.success(fieldMappingConvert(null, IndexMappingMetadata.getInstance(index), getMappingsResponse.mappings().get(index).getSourceAsMap().get("properties")));
                            }

                            @Override
                            public void onFailure(Exception e) {
                                sink.error(e);
                            }
                        });
                    }
                }));

    }

    private IndexMappingMetadata fieldMappingConvert(String baseKey, IndexMappingMetadata indexMappingMetaData, Object properties) {
        FastBeanCopier.copy(properties, new HashMap<String, Object>())
                .forEach((key, value) -> {
                    if (StringUtils.hasText(baseKey)) {
                        key = baseKey.concat(".").concat(key);
                    }
                    if (value instanceof Map) {
                        Map tempValue = FastBeanCopier.copy(value, new HashMap<>());
                        Object childProperties = tempValue.get("properties");
                        if (childProperties != null) {
                            fieldMappingConvert(key, indexMappingMetaData, childProperties);
                            return;
                        }
                        indexMappingMetaData.setMetadata(SingleMappingMetadata.builder()
                                .name(key)
                                .type(FieldType.of(tempValue.get("type")))
                                .build());
                    }
                });
        return indexMappingMetaData;
    }
}
