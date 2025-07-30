package org.jetlinks.community.device.measurements.message;

import org.jetlinks.community.micrometer.MeterRegistryCustomizer;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.device.timeseries.DeviceTimeSeriesMetric;
import org.jetlinks.community.micrometer.MeterRegistrySettings;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class DeviceMeterRegistryCustomizer implements MeterRegistryCustomizer {
    @Override
    public void custom(String metric, MeterRegistrySettings settings) {
        //给deviceMetrics添加产品等标签
        if (Objects.equals(metric, DeviceTimeSeriesMetric.deviceMetrics().getId())) {
            settings.addTag(PropertyConstants.productId.getKey(), StringType.GLOBAL);
        }
    }
}
