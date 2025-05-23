/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.elastic.search.index.strategies;

import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.PutMappingRequest;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.elastic.search.ElasticSearchSupport;
import org.jetlinks.community.elastic.search.enums.ElasticDateFormat;
import org.jetlinks.community.elastic.search.enums.ElasticPropertyType;
import org.jetlinks.community.elastic.search.index.DefaultElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexProperties;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexStrategy;
import org.jetlinks.community.elastic.search.service.reactive.ReactiveElasticsearchClient;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public abstract class AbstractElasticSearchIndexStrategy implements ElasticSearchIndexStrategy {
    @Getter
    private final String id;

    protected ReactiveElasticsearchClient client;

    protected ElasticSearchIndexProperties properties;

    protected String wrapIndex(String index) {
        return index.toLowerCase();
    }

    protected Mono<Boolean> indexExists(String index) {
        return client.execute(
            c -> c
                .indices()
                .exists(b -> b.index(index)).value());
    }

    protected Mono<Void> doCreateIndex(ElasticSearchIndexMetadata metadata) {
        return client
            .execute(c -> {
                c.indices()
                 .create(builder -> createIndexRequest(builder, metadata));
                return null;
            });
    }

    protected Mono<Void> doPutIndex(ElasticSearchIndexMetadata metadata,
                                    boolean justUpdateMapping) {
        String index = wrapIndex(metadata.getIndex());
        return this.indexExists(index)
                   .flatMap(exists -> {
                       if (exists) {
                           return doLoadIndexMetadata(index)
                               .flatMap(oldMapping -> {
                                   PutMappingRequest.Builder builder =
                                       createPutMappingRequest(metadata, oldMapping, new PutMappingRequest.Builder());
                                   //无需更新
                                   if (builder == null) {
                                       return Mono.empty();
                                   }
                                   return client
                                       .execute(c -> c.indices().putMapping(builder.build()));
                               })
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
        return client
            .execute(c -> {
                IndexMappingRecord record =
                    ElasticSearchSupport
                        .current()
                        .getIndexMapping(
                            c.indices()
                             .getMapping(b -> b
                                 .ignoreUnavailable(true)
                                 .allowNoIndices(true)
                                 .index(index)),
                            index
                        );
                if (record == null || record.mappings() == null) {
                    return null;
                }
                return convertMetadata(index, record.mappings());
            });
    }


    protected co.elastic.clients.elasticsearch.indices.CreateIndexRequest.Builder
    createIndexRequest(co.elastic.clients.elasticsearch.indices.CreateIndexRequest.Builder builder,
                       ElasticSearchIndexMetadata metadata) {
        builder.index(wrapIndex(metadata.getIndex()));

        builder.settings(properties::toSettings);

        builder.mappings(b -> {

            b.properties(createElasticProperties(metadata.getProperties()));

            b.dynamicTemplates(createDynamicTemplates());

            return b;
        });

        return builder;
    }

    private co.elastic.clients.elasticsearch.indices.PutMappingRequest.Builder
    createPutMappingRequest(ElasticSearchIndexMetadata metadata,
                            ElasticSearchIndexMetadata ignore,
                            co.elastic.clients.elasticsearch.indices.PutMappingRequest.Builder builder) {
        Map<String, Property> properties = createElasticProperties(metadata.getProperties());
        Map<String, Property> ignoreProperties = createElasticProperties(ignore.getProperties());
        for (Map.Entry<String, Property> en : ignoreProperties.entrySet()) {
            log.trace("ignore update index [{}] mapping property:{},{}", wrapIndex(metadata.getIndex()), en.getKey(), en
                .getValue());
            properties.remove(en.getKey());
        }
        if (properties.isEmpty()) {
            log.debug("ignore update index [{}] mapping", wrapIndex(metadata.getIndex()));
            return null;
        }
        List<PropertyMetadata> allProperties = new ArrayList<>();
        allProperties.addAll(metadata.getProperties());
        allProperties.addAll(ignore.getProperties());

        builder.index(wrapIndex(metadata.getIndex()));

        builder.properties(createElasticProperties(allProperties));

        return builder;
    }

    protected Map<String, Property> createElasticProperties(List<PropertyMetadata> metadata) {
        if (metadata == null) {
            return new HashMap<>();
        }
        return metadata
            .stream()
            .collect(Collectors.toMap(PropertyMetadata::getId,
                                      prop -> this.createElasticProperty(prop.getValueType()), (a, v) -> a));
    }

    protected Property createElasticProperty(DataType type) {

        if (type instanceof DateTimeType) {
            return Property.of(b -> b
                .date(b2 -> b2
                    .format(
                        ElasticDateFormat.getFormat(
                            ElasticDateFormat.epoch_millis,
                            ElasticDateFormat.strict_date_hour_minute_second,
                            ElasticDateFormat.strict_date_time,
                            ElasticDateFormat.strict_date)
                    )));

        } else if (type instanceof DoubleType) {

            return Property.of(b -> b.double_(b2 -> b2.nullValue(null)));

        } else if (type instanceof LongType) {
            return Property.of(b -> b.long_(b2 -> b2.nullValue(null)));
        } else if (type instanceof IntType) {
            return Property.of(b -> b.integer(b2 -> b2.nullValue(null)));
        } else if (type instanceof FloatType) {
            return Property.of(b -> b.float_(b2 -> b2.nullValue(null)));
        } else if (type instanceof BooleanType) {
            return Property.of(b -> b.boolean_(b2 -> b2.nullValue(null)));
        } else if (type instanceof GeoType) {
            return Property.of(b -> b.geoPoint(b2 -> b2));
        } else if (type instanceof GeoShapeType) {
            return Property.of(b -> b.geoShape(b2 -> b2));
        } else if (type instanceof ArrayType) {
            return createElasticProperty(((ArrayType) type).getElementType());
        } else if (type instanceof ObjectType objectType) {
            if (!CollectionUtils.isEmpty(objectType.getProperties())) {
                return Property.of(b -> b
                    .nested(b2 -> b2.properties(createElasticProperties(objectType.getProperties()))));
            }
            return Property.of(b -> b.nested(b2 -> b2));
        } else {
            int above = Optional
                .ofNullable(type)
                .flatMap(_type -> _type.getExpand(ConfigMetadataConstants.maxLength.getKey()))
                .filter(val -> val instanceof Number || StringUtils.isNumeric(String.valueOf(val)))
                .map(CastUtils::castNumber)
                .map(Number::intValue)
                .orElse(properties.getKeywordIgnoreAbove());

            return Property.of(b -> b.keyword(b2 -> b2.ignoreAbove(above)));
        }
    }

    protected ElasticSearchIndexMetadata convertMetadata(String index, TypeMapping mapping) {
        Map<String, Property> properties = mapping.properties();


        return new DefaultElasticSearchIndexMetadata(index, convertProperties(properties));
    }

    @SuppressWarnings("all")
    protected List<PropertyMetadata> convertProperties(Map<String, Property> properties) {
        return properties
            .entrySet()
            .stream()
            .map(entry -> convertProperty(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

    }

    private PropertyMetadata convertProperty(String property, Property prop) {
        SimplePropertyMetadata metadata = new SimplePropertyMetadata();
        metadata.setId(property);
        metadata.setName(property);
        ElasticPropertyType elasticPropertyType = ElasticPropertyType.of(prop._kind().jsonValue());
        if (null != elasticPropertyType) {
            DataType dataType = elasticPropertyType.getType();
            if ((elasticPropertyType == ElasticPropertyType.OBJECT
                || elasticPropertyType == ElasticPropertyType.NESTED)
                && dataType instanceof ObjectType) {
                ObjectType objectType = ((ObjectType) dataType);
                objectType.setProperties(convertProperties(prop.nested().properties()));
            }
            metadata.setValueType(dataType);
        } else {
            metadata.setValueType(StringType.GLOBAL);
        }
        return metadata;
    }


    protected List<Map<String, DynamicTemplate>> createDynamicTemplates() {

        List<Map<String, DynamicTemplate>> list = new ArrayList<>();
        {
            list.add(Collections.singletonMap(
                "string_fields", ElasticSearchSupport
                    .current()
                    .createDynamicTemplate(
                        "string",
                        createElasticProperty(StringType.GLOBAL))));

        }
        {
            list.add(Collections.singletonMap(
                "date_fields", ElasticSearchSupport
                    .current()
                    .createDynamicTemplate(
                        "string",
                        createElasticProperty(StringType.GLOBAL))));
        }

        return list;
    }
}
