package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.metadata.unit.ValueUnit;
import org.jetlinks.community.things.data.ThingPropertyDetail;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceProperty implements Serializable {
    private static final long serialVersionUID = -1L;

    @Schema(description = "ID")
    private String id;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "属性ID")
    private String property;

    @Schema(description = "属性名")
    private String propertyName;

    @Schema(description = "类型")
    private String type;

    @Schema(description = "单位")
    private String unit;

    @Hidden
    private Object numberValue;

    @Hidden
    private Object objectValue;

    @Hidden
    private Date timeValue;

    @Hidden
    private String stringValue;

    @Hidden
    private GeoPoint geoValue;

    @Schema(description = "属性值")
    private Object value;

    @Schema(description = "格式化值")
    private Object formatValue;

    @Schema(description = "创建时间")
    private long createTime;

    @Schema(description = "数据时间")
    private long timestamp;

    @Schema(description = "格式化后的时间,在聚合查询时此字段有值")
    private String formatTime;

    /**
     * 设备状态值,如果是查询的数据库,此字段可能为{@link null}
     *
     * @see ReportPropertyMessage#getPropertyStates()
     */
    @Schema(description = "状态值")
    private String state;

    public DeviceProperty deviceId(String deviceId) {
        this.deviceId = deviceId;
        return this;
    }

    public DeviceProperty property(String property) {
        this.property = property;
        return this;
    }

    public DeviceProperty formatTime(String formatTime) {
        this.formatTime = formatTime;
        return this;
    }

    public DeviceProperty withProperty(PropertyMetadata metadata) {

        if (metadata != null) {
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
                if (type instanceof ObjectType) {
                    setObjectValue(value);
                }
                if (type instanceof GeoType && value instanceof GeoPoint) {
                    setGeoValue(((GeoPoint) value));
                }
                if (type instanceof DateTimeType && value instanceof Date) {
                    setTimeValue(((Date) value));
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

    public static DeviceProperty of(TimeSeriesData data,
                                    Object value,
                                    PropertyMetadata metadata) {
        DeviceProperty deviceProperty = data.as(DeviceProperty.class);
        deviceProperty.setCreateTime(data.getLong("createTime", data.getTimestamp()));
        deviceProperty.setTimestamp(data.getTimestamp());
        deviceProperty.setValue(value);
        return deviceProperty.withProperty(metadata);

    }

    public static DeviceProperty of(Object value,
                                    PropertyMetadata metadata) {
        DeviceProperty property = new DeviceProperty();
        property.setTimestamp(System.currentTimeMillis());
        property.setCreateTime(property.getTimestamp());
        property.setValue(value);
        return property.withProperty(metadata);

    }

    public static DeviceProperty of(AggregationData data,
                                    PropertyMetadata metadata) {
        DeviceProperty property = data.as(DeviceProperty.class);
        return property.withProperty(metadata);

    }

    public static DeviceProperty of(TimeSeriesData timeSeriesData,
                                    PropertyMetadata metadata) {
        DeviceProperty property = timeSeriesData.as(DeviceProperty.class);
        property.setTimestamp(timeSeriesData.getTimestamp());
        return property.withProperty(metadata);

    }

    public static DeviceProperty of(ThingPropertyDetail detail) {
        DeviceProperty deviceProperty = FastBeanCopier.copy(detail, new DeviceProperty());
        deviceProperty.setDeviceId(detail.getThingId());
        return deviceProperty;
    }

    public DeviceProperty generateId() {
        if (StringUtils.isEmpty(id)) {
            setId(DigestUtils.md5Hex(String.join("", deviceId, property, String.valueOf(timestamp))));
        }
        return this;
    }
}
