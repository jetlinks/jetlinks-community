package org.jetlinks.community.things.data;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.core.things.ThingProperty;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
@Generated
public class ThingPropertyDetail implements ThingProperty {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "物实例ID")
    private String thingId;

    @Schema(description = "属性ID")
    private String property;

    @Schema(description = "状态")
    private String state;

    @Schema(description = "属性值")
    private Object value;

    @Schema(description = "数字值")
    private Object numberValue;

    @Schema(description = "格式化后的值")
    private Object formatValue;

    @Schema(description = "属性名")
    private String propertyName;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "时间戳")
    private long timestamp;

    @Schema(description = "创建时间")
    private long createTime;

    @Schema(description = "格式化后的时间")
    private String formatTime;


    public ThingPropertyDetail property(String property) {
        this.property = property;
        return this;
    }

    public ThingPropertyDetail thingId(String thingId) {
        this.thingId = thingId;
        return this;
    }

    public ThingPropertyDetail timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ThingPropertyDetail createTime(long createTime) {
        this.createTime = createTime;
        return this;
    }

    public ThingPropertyDetail formatTime(String formatTime) {
        this.formatTime = formatTime;
        return this;
    }

    public ThingPropertyDetail withProperty(PropertyMetadata metadata) {

        if (metadata != null) {
            setProperty(metadata.getId());
            setPropertyName(metadata.getName());
            DataType type = metadata.getValueType();
            Object value = this.getValue();
            try {
                if (type instanceof NumberType) {
                    NumberType<?> numberType = ((NumberType<?>) type);

                    Number numberValue = NumberType
                        .convertScaleNumber(value,
                                            numberType.getScale(),
                                            numberType.getRound(),
                                            Function.identity());

                    if (numberValue != null) {
                        this.setValue(value = numberValue);
                    }
                    this.setNumberValue(numberValue);
                } else if (type instanceof Converter) {
                    value = ((Converter<?>) type).convert(value);
                    this.setValue(value);
                }
                this.setFormatValue(type.format(value));
            } catch (Exception ignore) {

            }
            if (type instanceof UnitSupported) {
                UnitSupported unitSupported = (UnitSupported) type;
                this.setUnit(Optional.ofNullable(unitSupported.getUnit())
                                     .map(ValueUnit::getSymbol)
                                     .orElse(null));
            }
            this.setType(type.getType());
        }
        return this;
    }

    public static ThingPropertyDetail of(TimeSeriesData data,
                                         Object value,
                                         PropertyMetadata metadata) {
        ThingPropertyDetail deviceProperty = data.as(ThingPropertyDetail.class);
        deviceProperty.setCreateTime(data.getLong("createTime", data.getTimestamp()));
        deviceProperty.setTimestamp(data.getTimestamp());
        deviceProperty.setValue(value);
        return deviceProperty.withProperty(metadata);

    }

    public static ThingPropertyDetail of(Object value,
                                         PropertyMetadata metadata) {
        ThingPropertyDetail property = new ThingPropertyDetail();
        property.setTimestamp(System.currentTimeMillis());
        property.setCreateTime(property.getTimestamp());
        property.setValue(value);
        return property.withProperty(metadata);

    }

    public static ThingPropertyDetail of(AggregationData data,
                                         PropertyMetadata metadata) {
        ThingPropertyDetail property = data.as(ThingPropertyDetail.class);
        return property.withProperty(metadata);

    }

    @Nullable
    public static ThingPropertyDetail of(TimeSeriesData timeSeriesData,
                                         PropertyMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        ThingPropertyDetail property = timeSeriesData.as(ThingPropertyDetail.class);
        property.setTimestamp(timeSeriesData.getTimestamp());
        return property
            .withProperty(metadata)
            .generateId();

    }

    public ThingPropertyDetail generateId() {
        if (!StringUtils.hasText(id)) {
            setId(DigestUtils.md5Hex(String.join("", thingId, property, String.valueOf(timestamp))));
        }
        return this;
    }

    @Override
    public String toString() {
        return timestamp + " : " + property + " = " + value;
    }
}
