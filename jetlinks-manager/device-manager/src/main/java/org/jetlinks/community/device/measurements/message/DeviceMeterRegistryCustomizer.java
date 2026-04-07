/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
