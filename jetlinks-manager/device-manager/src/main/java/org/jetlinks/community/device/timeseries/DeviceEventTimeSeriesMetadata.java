package org.jetlinks.community.device.timeseries;

import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

import java.util.ArrayList;
import java.util.List;

class DeviceEventTimeSeriesMetadata implements TimeSeriesMetadata {

    public final TimeSeriesMetric metric;

    private static final List<PropertyMetadata> defaultMetadata = new ArrayList<>();

    static {
        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("productId");
            property.setValueType(new StringType());
            property.setName("型号ID");
            defaultMetadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("orgId");
            property.setValueType(new StringType());
            property.setName("租户ID");
            defaultMetadata.add(property);
        }
        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("deviceId");
            property.setValueType(new StringType());
            property.setName("设备ID");
            defaultMetadata.add(property);
        }
    }

    private final List<PropertyMetadata> metadata = new ArrayList<>(defaultMetadata);

    public DeviceEventTimeSeriesMetadata(String productId, EventMetadata eventMetadata) {
        metric = DeviceTimeSeriesMetric.deviceEventMetric(productId, eventMetadata.getId());
        DataType type = eventMetadata.getType();
        if (type instanceof ObjectType) {
            if (CollectionUtils.isNotEmpty(((ObjectType) type).getProperties())) {
                metadata.addAll(((ObjectType) type).getProperties());
            }
        } else {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("value");
            property.setValueType(type);
            property.setName("数据");
            metadata.add(property);
        }
    }

    @Override
    public TimeSeriesMetric getMetric() {
        return metric;
    }

    @Override
    public List<PropertyMetadata> getProperties() {
        return metadata;
    }
}
