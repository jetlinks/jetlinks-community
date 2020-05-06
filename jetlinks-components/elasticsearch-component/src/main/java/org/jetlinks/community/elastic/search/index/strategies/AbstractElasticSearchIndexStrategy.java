package org.jetlinks.community.elastic.search.index.strategies;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.elastic.search.ElasticRestClient;
import org.jetlinks.community.elastic.search.enums.ElasticDateFormat;
import org.jetlinks.community.elastic.search.enums.ElasticPropertyType;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexStrategy;
import org.jetlinks.community.elastic.search.utils.ReactorActionListener;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public abstract class AbstractElasticSearchIndexStrategy implements ElasticSearchIndexStrategy {
    @Getter
    private String id;

    protected ElasticRestClient client;

    protected ElasticSearchIndexProperties properties;

    protected String wrapIndex(String index) {
        return index.toLowerCase();
    }

    protected Mono<Boolean> indexExists(String index) {
        return ReactorActionListener.mono(
            actionListener ->
                client.getQueryClient()
                    .indices()
                    .existsAsync(new GetIndexRequest(wrapIndex(index)), RequestOptions.DEFAULT, actionListener));
    }

    protected Mono<Void> doCreateIndex(ElasticSearchIndexMetadata metadata) {
        return ReactorActionListener.<CreateIndexResponse>mono(
            actionListener -> client.getQueryClient()
                .indices()
                .createAsync(createIndexRequest(metadata), RequestOptions.DEFAULT, actionListener))
            .then();
    }

    protected Mono<Void> doPutIndex(ElasticSearchIndexMetadata metadata,
                                    boolean justUpdateMapping) {
        String index = wrapIndex(metadata.getIndex());
        return this.indexExists(index)
            .flatMap(exists -> {
                if (exists) {
                    return doLoadIndexMetadata(index)
                        .flatMap(oldMapping -> Mono.justOrEmpty(createPutMappingRequest(metadata, oldMapping)))
                        .flatMap(request -> ReactorActionListener.<AcknowledgedResponse>mono(
                            actionListener ->
                                client.getWriteClient()
                                    .indices()
                                    .putMappingAsync(request, RequestOptions.DEFAULT, actionListener)))
                        .then();
                }
                if (justUpdateMapping) {
                    return Mono.empty();
                }
                return doCreateIndex(metadata);
            });
    }

    protected Mono<ElasticSearchIndexMetadata> doLoadIndexMetadata(String _index) {
        String index = wrapIndex(_index);
        return ReactorActionListener
            .<GetMappingsResponse>mono(listener -> client.getQueryClient()
                .indices()
                .getMappingAsync(new GetMappingsRequest().indices(index), RequestOptions.DEFAULT, listener))
            .flatMap(resp -> Mono.justOrEmpty(convertMetadata(index, resp.mappings().get(index))));
    }


    public CreateIndexRequest createIndexRequest(ElasticSearchIndexMetadata metadata) {
        CreateIndexRequest request = new CreateIndexRequest(wrapIndex(metadata.getIndex()));
        request.settings(properties.toSettings());
        Map<String, Object> mappingConfig = new HashMap<>();
        mappingConfig.put("properties", createElasticProperties(metadata.getProperties()));
        mappingConfig.put("dynamic_templates", createDynamicTemplates());
        request.mapping(mappingConfig);
        return request;
    }

    private PutMappingRequest createPutMappingRequest(ElasticSearchIndexMetadata metadata, ElasticSearchIndexMetadata ignore) {
        Map<String, Object> properties = createElasticProperties(metadata.getProperties());
        Map<String, Object> ignoreProperties = createElasticProperties(ignore.getProperties());
        for (Map.Entry<String, Object> en : ignoreProperties.entrySet()) {
            log.trace("ignore update index [{}] mapping property:{},{}", wrapIndex(metadata.getIndex()), en.getKey(), en.getValue());
            properties.remove(en.getKey());
        }
        if (properties.isEmpty()) {
            log.debug("ignore update index [{}] mapping", wrapIndex(metadata.getIndex()));
            return null;
        }
        PutMappingRequest request = new PutMappingRequest(wrapIndex(metadata.getIndex()));
        request.source(Collections.singletonMap("properties", properties));
        return request;
    }

    protected Map<String, Object> createElasticProperties(List<PropertyMetadata> metadata) {
        if (metadata == null) {
            return new HashMap<>();
        }
        return metadata.stream()
            .collect(Collectors.toMap(PropertyMetadata::getId, prop -> this.createElasticProperty(prop.getValueType())));
    }

    protected Map<String, Object> createElasticProperty(DataType type) {
        Map<String, Object> property = new HashMap<>();
        if (type instanceof DateTimeType) {
            property.put("type", "date");
            property.put("format", ElasticDateFormat.getFormat(
                ElasticDateFormat.epoch_millis,
                ElasticDateFormat.strict_date_hour_minute_second,
                ElasticDateFormat.strict_date_time,
                ElasticDateFormat.strict_date)
            );
        } else if (type instanceof DoubleType) {
            property.put("type", "double");
        } else if (type instanceof LongType) {
            property.put("type", "long");
        } else if (type instanceof IntType) {
            property.put("type", "integer");
        } else if (type instanceof FloatType) {
            property.put("type", "float");
        } else if (type instanceof BooleanType) {
            property.put("type", "boolean");
        } else if (type instanceof GeoType) {
            property.put("type", "geo_point");
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = ((ArrayType) type);
            return createElasticProperty(arrayType.getElementType());
        } else if (type instanceof ObjectType) {
            property.put("type", "nested");
            ObjectType objectType = ((ObjectType) type);
            property.put("properties", createElasticProperties(objectType.getProperties()));
        } else {
            property.put("type", "keyword");
        }
        return property;
    }

    protected ElasticSearchIndexMetadata convertMetadata(String index, MappingMetaData metaData) {
        Map<String, Object> response = metaData.getSourceAsMap();
        Object properties = response.get("properties");
        return new DefaultElasticSearchIndexMetadata(index, convertProperties(properties));
    }

    @SuppressWarnings("all")
    protected List<PropertyMetadata> convertProperties(Object properties) {
        if (properties == null) {
            return new ArrayList<>();
        }
        return ((Map<String, Map<String, Object>>) properties)
            .entrySet()
            .stream()
            .map(entry -> convertProperty(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    private PropertyMetadata convertProperty(String property, Map<String, Object> map) {
        String type = String.valueOf(map.get("type"));
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setId(property);
        metadata.setName(property);
        ElasticPropertyType elasticPropertyType = ElasticPropertyType.of(type);
        if (null != elasticPropertyType) {
            DataType dataType = elasticPropertyType.getType();
            if ((elasticPropertyType == ElasticPropertyType.OBJECT
                || elasticPropertyType == ElasticPropertyType.NESTED)
                && dataType instanceof ObjectType) {
                @SuppressWarnings("all")
                Map<String, Object> nestProperties = (Map<String, Object>) map.get("properties");
                if (null != nestProperties) {
                    ObjectType objectType = ((ObjectType) dataType);
                    objectType.setProperties(convertProperties(nestProperties));
                }
            }
            metadata.setValueType(dataType);
        } else {
            metadata.setValueType(new StringType());
        }
        return metadata;
    }

    protected List<?> createDynamicTemplates() {
        List<Map<String, Object>> maps = new ArrayList<>();
        {
            Map<String, Object> config = new HashMap<>();
            config.put("match_mapping_type", "string");
            config.put("mapping", createElasticProperty(new StringType()));
            maps.add(Collections.singletonMap("string_fields", config));
        }
        {
            Map<String, Object> config = new HashMap<>();
            config.put("match_mapping_type", "date");
            config.put("mapping", createElasticProperty(new DateTimeType()));
            maps.add(Collections.singletonMap("date_fields", config));
        }

        return maps;
    }

}
