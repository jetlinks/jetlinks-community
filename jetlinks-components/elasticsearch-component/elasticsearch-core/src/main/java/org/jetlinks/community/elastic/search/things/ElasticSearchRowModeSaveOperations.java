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
package org.jetlinks.community.elastic.search.things;

import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.things.ThingsRegistry;
import org.jetlinks.community.elastic.search.service.ElasticSearchService;
import org.jetlinks.community.things.data.operations.DataSettings;
import org.jetlinks.community.things.data.operations.MetricBuilder;
import org.jetlinks.community.things.data.operations.RowModeSaveOperationsBase;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ObjectMappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.jetlinks.community.things.data.ThingsDataConstants.*;

class ElasticSearchRowModeSaveOperations extends RowModeSaveOperationsBase {

    private final ElasticSearchService searchService;

    public ElasticSearchRowModeSaveOperations(ThingsRegistry registry,
                                              MetricBuilder metricBuilder,
                                              DataSettings settings,
                                              ElasticSearchService searchService) {
        super(registry, metricBuilder, settings);
        this.searchService = searchService;
    }

    @Override
    protected Mono<Void> doSave(String metric, TimeSeriesData data) {
        return searchService.commit(metric, data.getData());
    }

    @Override
    protected Mono<Void> doSave(String metric, Flux<TimeSeriesData> data) {
        return searchService.save(metric, data.map(TimeSeriesData::getData));
    }

    @Override
    protected void fillRowPropertyValue(Map<String, Object> target, PropertyMetadata property, Object value) {
        DataType type = property.getValueType();
        if (type instanceof ArrayType && value != null) {
            if (propertyIsJsonStringStorage(property)) {
                String convertedValue = value instanceof String
                    ? String.valueOf(value)
                    : ObjectMappers.toJsonString(value);
                target.put(COLUMN_PROPERTY_VALUE, convertedValue);
                return;
            }

            ArrayType objectType = (ArrayType) type;
            DataType elementType = objectType.getElementType();
            if (elementType instanceof ObjectType) {
                Object val = objectType.convert(value);
                target.put(COLUMN_PROPERTY_OBJECT_VALUE, val);
                target.put(COLUMN_PROPERTY_VALUE, ObjectMappers.toJsonString(val));
                return;
            }
        }
        super.fillRowPropertyValue(target, property, value);
    }
}
