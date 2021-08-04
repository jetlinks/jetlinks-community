package org.jetlinks.community.timeseries.micrometer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DoubleType;
import org.jetlinks.core.metadata.types.StringType;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor(staticName = "of")
class MeterTimeSeriesMetadata implements TimeSeriesMetadata {
    @Getter
    private TimeSeriesMetric metric;

    @Getter
    private List<String> keys;

    static final List<PropertyMetadata> properties = new ArrayList<>();

    static {

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("name");
            property.setName("名称");
            property.setValueType(new StringType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("type");
            property.setName("类型");
            property.setValueType(new StringType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("count");
            property.setName("计数");
            property.setValueType(new DoubleType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("value");
            property.setName("值");
            property.setValueType(new DoubleType());
            properties.add(property);
        }


        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("max");
            property.setName("最大值");
            property.setValueType(new DoubleType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("total");
            property.setName("总计");
            property.setValueType(new DoubleType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("active");
            property.setName("活跃数");
            property.setValueType(new DoubleType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("unknown");
            property.setName("未知");
            property.setValueType(new DoubleType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("sum");
            property.setName("合计");
            property.setValueType(new DoubleType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("mean");
            property.setName("平均值");
            property.setValueType(new DoubleType());
            properties.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("duration");
            property.setName("期间");
            property.setValueType(new DoubleType());
            properties.add(property);
        }
    }

    @Override
    public List<PropertyMetadata> getProperties() {

        List<PropertyMetadata> metadata = new ArrayList<>(properties);
        for (String key : keys) {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId(key);
            property.setName(key);
            property.setValueType(new StringType());
            metadata.add(property);
        }
        return metadata;
    }
}
