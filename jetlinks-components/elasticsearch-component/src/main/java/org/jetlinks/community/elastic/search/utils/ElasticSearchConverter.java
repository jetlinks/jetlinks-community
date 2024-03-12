package org.jetlinks.community.elastic.search.utils;

import com.google.common.collect.Collections2;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.jetlinks.community.elastic.search.index.ElasticSearchIndexMetadata;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;

import java.util.*;

public class ElasticSearchConverter {


    public static SearchSourceBuilder convertSearchSourceBuilder(QueryParam queryParam, ElasticSearchIndexMetadata metadata) {
        return QueryParamTranslator.convertSearchSourceBuilder(queryParam, metadata);
    }

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
            }else if (type instanceof GeoShapeType) {
                newData.put(property.getId(),GeoShape.of(val).toMap());
            }  else if (type instanceof DateTimeType) {
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