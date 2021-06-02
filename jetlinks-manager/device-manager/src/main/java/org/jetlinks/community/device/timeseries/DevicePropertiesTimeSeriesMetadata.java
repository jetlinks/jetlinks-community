package org.jetlinks.community.device.timeseries;

import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.StringType;

import java.util.ArrayList;
import java.util.List;

class DevicePropertiesTimeSeriesMetadata implements TimeSeriesMetadata {

    private final static List<PropertyMetadata> metadata = new ArrayList<>();

    private final TimeSeriesMetric metric;

    public DevicePropertiesTimeSeriesMetadata(String productId) {
        this.metric = DeviceTimeSeriesMetric.devicePropertyMetric(productId);
    }

    static {

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("property");
            property.setValueType(new StringType());
            property.setName("属性标识");
            metadata.add(property);
        }
        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("propertyName");
            property.setValueType(new StringType());
            property.setName("属性名称");
            metadata.add(property);
        }
        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("value");
            property.setValueType(new StringType());
            property.setName("原始值");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("objectValue");
            property.setValueType(new ObjectType());
            property.setName("对象值");
            metadata.add(property);
        }
        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("timeValue");
            property.setValueType(new DateTimeType());
            property.setName("时间值");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("formatValue");
            property.setValueType(new StringType());
            property.setName("格式化的值");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("numberValue");
            property.setValueType(new DoubleType());
            property.setName("数字值");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("deviceId");
            property.setValueType(new StringType());
            property.setName("设备ID");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("productId");
            property.setValueType(new StringType());
            property.setName("型号ID");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("orgId");
            property.setValueType(new StringType());
            property.setName("组织ID");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("createTime");
            property.setValueType(new DateTimeType());
            property.setName("创建事件");
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
