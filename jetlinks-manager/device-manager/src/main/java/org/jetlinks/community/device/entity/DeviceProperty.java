package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.GeoPoint;
import org.jetlinks.core.metadata.types.NumberType;
import org.jetlinks.core.metadata.types.ObjectType;

import java.io.Serializable;

@Getter
@Setter
public class DeviceProperty implements Serializable {
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

    @Hidden
    private Object numberValue;

    @Hidden
    private Object objectValue;

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
                if (type instanceof Converter) {
                    value = ((Converter<?>) type).convert(value);
                    this.setValue(value);
                }
                if (type instanceof NumberType) {
                    setNumberValue(value);
                }
                if (type instanceof ObjectType) {
                    setObjectValue(value);
                }

                this.setFormatValue(type.format(value));
            } catch (Exception ignore) {

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
}
