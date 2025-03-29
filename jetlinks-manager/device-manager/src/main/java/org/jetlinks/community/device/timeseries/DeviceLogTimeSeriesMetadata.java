package org.jetlinks.community.device.timeseries;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

import java.util.ArrayList;
import java.util.List;

class DeviceLogTimeSeriesMetadata implements TimeSeriesMetadata {

    private final static List<PropertyMetadata> metadata = new ArrayList<>();

    private final TimeSeriesMetric metric;

    public DeviceLogTimeSeriesMetadata(String productId) {
        this.metric = DeviceTimeSeriesMetric.deviceLogMetric(productId);
    }

    static {

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("id");
            property.setValueType(new StringType());
            property.setName("ID");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("type");
            property.setValueType(new StringType());
            property.setName("日志类型");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("content");
            property.setValueType(new StringType().expand(
                ConfigMetadataConstants.maxLength,
                Long.getLong("jetlinks.device.log.content.max-length",4096L)));
            property.setName("日志内容");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("deviceId");
            property.setValueType(StringType.GLOBAL);
            property.setName("设备ID");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("productId");
            property.setValueType(StringType.GLOBAL);
            property.setName("产品ID");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("messageId");
            property.setValueType(StringType.GLOBAL);
            property.setName("消息ID");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("createTime");
            property.setValueType(DateTimeType.GLOBAL);
            property.setName("创建时间");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("timestamp");
            property.setValueType(DateTimeType.GLOBAL);
            property.setName("数据时间");
            metadata.add(property);
        }

    }

    @Override
    public TimeSeriesMetric getMetric() {
        return metric;
    }

    @Override
    public List<PropertyMetadata> getProperties() {
        return new ArrayList<>(metadata);
    }
}
