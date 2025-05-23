/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.device.timeseries;

import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.device.service.data.StorageConstants;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetric;

import java.util.ArrayList;
import java.util.List;

class FixedPropertiesTimeSeriesMetadata implements TimeSeriesMetadata {

    private final static List<PropertyMetadata> metadata = new ArrayList<>();

    private final TimeSeriesMetric metric;

    private final List<PropertyMetadata> fixed;

    public FixedPropertiesTimeSeriesMetadata(String productId, List<PropertyMetadata> fixed) {
        this.metric = DeviceTimeSeriesMetric.devicePropertyMetric(productId);
        this.fixed = new ArrayList<>(fixed.size() + metadata.size());
        for (PropertyMetadata propertyMetadata : fixed) {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId(propertyMetadata.getId());
            property.setName(propertyMetadata.getName());
            if (StorageConstants.propertyIsJsonStringStorage(propertyMetadata)) {
                property.setValueType(StringType.GLOBAL);
            } else {
                property.setValueType(propertyMetadata.getValueType());
            }
            this.fixed.add(property);
        }
        this.fixed.addAll(metadata);
    }

    static {

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("id");
            property.setValueType(StringType.GLOBAL);
            property.setName("id");
            metadata.add(property);
        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("deviceId");
            property.setValueType(new StringType());
            property.setName("设备ID");
            metadata.add(property);
        }

//        {
//            SimplePropertyMetadata property = new SimplePropertyMetadata();
//            property.setId("productId");
//            property.setValueType(new StringType());
//            property.setName("产品ID");
//            metadata.add(property);
//        }

        {
            SimplePropertyMetadata property = new SimplePropertyMetadata();
            property.setId("createTime");
            property.setValueType(DateTimeType.GLOBAL);
            property.setName("创建时间");
            metadata.add(property);
        }
    }

    @Override
    public TimeSeriesMetric getMetric() {
        return metric;
    }

    @Override
    public List<PropertyMetadata> getProperties() {
        return new ArrayList<>(fixed);
    }
}
