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
package org.jetlinks.community.elastic.search.utils;

import com.google.common.collect.Collections2;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;

import java.util.*;

public class ElasticSearchConverter {


    public static Map<String, Object> convertDataToElastic(Map<String, Object> data,
                                                           List<PropertyMetadata> properties) {
        Map<String, Object> newValue = new HashMap<>(data);
        for (PropertyMetadata property : properties) {
            DataType type = property.getValueType();
            Object val = data.get(property.getId());
            if (val == null) {
                continue;
            }
            //处理地理位置类型
            if (type instanceof GeoType) {
                GeoPoint point = ((GeoType) type).convert(val);

                Map<String, Object> geoData = new HashMap<>();
                geoData.put("lat", point.getLat());
                geoData.put("lon", point.getLon());

                newValue.put(property.getId(), geoData);
            } else if (type instanceof GeoShapeType) {
                GeoShape shape = ((GeoShapeType) type).convert(val);
                if (shape == null) {
                    throw new UnsupportedOperationException("不支持的GeoShape格式:" + val);
                }
                Map<String, Object> geoData = new HashMap<>();
                geoData.put("type", shape.getType().name());
                geoData.put("coordinates", shape.getCoordinates());
                newValue.put(property.getId(), geoData);
            } else if (type instanceof DateTimeType) {
                Date date = ((DateTimeType) type).convert(val);
                newValue.put(property.getId(), date.getTime());
            } else if (type instanceof Converter) {
                newValue.put(property.getId(), ((Converter<?>) type).convert(val));
            }
        }
        return newValue;
    }

    public static Map<String, Object> convertDataFromElastic(Map<String, Object> data,
                                                             List<PropertyMetadata> properties) {
        Map<String, Object> newData = new HashMap<>(data);
        for (PropertyMetadata property : properties) {
            DataType type = property.getValueType();
            Object val = newData.get(property.getId());
            if (val == null) {
                continue;
            }
            //处理地理位置类型
            if (type instanceof GeoType) {
                newData.put(property.getId(), ((GeoType) type).convertToMap(val));
            } else if (type instanceof GeoShapeType) {
                newData.put(property.getId(), GeoShape.of(val).toMap());
            } else if (type instanceof DateTimeType) {
                Date date = ((DateTimeType) type).convert(val);
                newData.put(property.getId(), date);
            } else if (type instanceof ObjectType) {
                if (val instanceof Collection) {
                    val = Collections2.transform(((Collection<?>) val), ((ObjectType) type)::convert);
                    newData.put(property.getId(), val);
                } else {
                    newData.put(property.getId(), ((ObjectType) type).convert(val));
                }
            } else if (type instanceof Converter) {
                newData.put(property.getId(), ((Converter<?>) type).convert(val));
            }
        }
        return newData;
    }
}
