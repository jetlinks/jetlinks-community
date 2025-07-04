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
package org.jetlinks.community.device.entity;

import com.alibaba.fastjson.JSON;
import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class DevicePropertiesEntity {

    private String id;

    private String deviceId;

    private String property;

    private String propertyName;

    private String stringValue;

    private String formatValue;

    private BigDecimal numberValue;

    private GeoPoint geoValue;

    private long timestamp;

    private Object objectValue;

    private String value;

    @Deprecated
    private String orgId;

    @Deprecated
    private String productId;

    private Date timeValue;

    private String type;

    private long createTime;

    public Map<String, Object> toMap() {
        return FastBeanCopier.copy(this, new HashMap<>(22));
    }

    public DevicePropertiesEntity withValue(PropertyMetadata metadata, Object value) {
        if (metadata == null) {
            setValue(String.valueOf(value));
            if (value instanceof Number) {
                numberValue = new BigDecimal(value.toString());
            } else if (value instanceof Date) {
                timeValue = ((Date) value);
            }
            return this;
        }
        setProperty(metadata.getId());
//        setPropertyName(metadata.getName());
        return withValue(metadata.getValueType(), value);

    }

    public DevicePropertiesEntity withValue(DataType type, Object value) {
        if (value == null) {
            return this;
        }
        setType(type.getType());
        String convertedValue;

        if (type instanceof NumberType) {
            NumberType<?> numberType = (NumberType<?>) type;
            Number number = numberType.convertNumber(value);
            if (number == null) {
                throw new BusinessException("error.cannot_convert" , 500, value , type.getId());
            }
            convertedValue = number.toString();
            BigDecimal numberVal;
            if (number instanceof BigDecimal) {
                numberVal = ((BigDecimal) number);
            } else if (number instanceof Integer) {
                numberVal = BigDecimal.valueOf(number.intValue());
            } else if (number instanceof Long) {
                numberVal = BigDecimal.valueOf(number.longValue());
            } else {
                numberVal = BigDecimal.valueOf(number.doubleValue());
            }
            setNumberValue(numberVal);
        } else if (type instanceof DateTimeType) {
            DateTimeType dateTimeType = (DateTimeType) type;
            convertedValue = String.valueOf(value);
            setTimeValue(dateTimeType.convert(value));
        } else if (type instanceof ObjectType) {
            ObjectType objectType = (ObjectType) type;
            Object val = objectType.convert(value);
            convertedValue = JSON.toJSONString(val);
            setObjectValue(val);
        } else if (type instanceof ArrayType) {
            ArrayType objectType = (ArrayType) type;
            Object val = objectType.convert(value);
            convertedValue = JSON.toJSONString(val);
            setObjectValue(val);
        } else if (type instanceof GeoType) {
            GeoType geoType = (GeoType) type;
            GeoPoint val = geoType.convert(value);
            convertedValue = String.valueOf(val);
            setGeoValue(val);
        } else {
            setStringValue(convertedValue = String.valueOf(value));
        }
        setValue(convertedValue);

//        ofNullable(type.format(value))
//            .map(String::valueOf)
//            .ifPresent(this::setFormatValue);

        return this;
    }
}
