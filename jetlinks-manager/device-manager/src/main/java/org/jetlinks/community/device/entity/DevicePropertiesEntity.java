package org.jetlinks.community.device.entity;

import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevicePropertiesEntity {

    public String id;

    private String deviceId;

    private String property;

    private String propertyName;

    private String stringValue;

    private String formatValue;

    private BigDecimal numberValue;

    private long timestamp;

    private Object objectValue;

    private String value;

    private String orgId;

    private String productId;

    private Date timeValue;


    public Map<String, Object> toMap() {
        return FastBeanCopier.copy(this, HashMap::new);
    }

    public DevicePropertiesEntity withValue(PropertyMetadata metadata, Object value) {
        if (metadata == null) {
            return this;
        }
        setProperty(metadata.getId());
        setPropertyName(metadata.getName());
        return withValue(metadata.getValueType(), value);

    }

    public DevicePropertiesEntity withValue(DataType type, Object value) {
        if (value == null) {
            return this;
        }
        if (type instanceof NumberType) {
            NumberType<?> numberType = (NumberType<?>) type;
            setNumberValue(new BigDecimal(numberType.convertNumber(value).toString()));
        } else if (type instanceof DateTimeType) {
            DateTimeType dateTimeType = (DateTimeType) type;
            setTimeValue(dateTimeType.convert(value));
        } else if (type instanceof ObjectType) {
            ObjectType ObjectType = (ObjectType) type;
            setObjectValue(ObjectType.convert(value));
        } else {
            setStringValue(String.valueOf(value));
        }
        ofNullable(type.format(value))
            .map(String::valueOf)
            .ifPresent(this::setFormatValue);

        return this;
    }
}
