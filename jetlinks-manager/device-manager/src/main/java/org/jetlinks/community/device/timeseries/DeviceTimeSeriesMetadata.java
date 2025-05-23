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

import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;

import java.util.List;

/**
 * 设备相关时序数据库元数据定义
 *
 * @author zhouhao
 * @since 1.0
 */
public interface DeviceTimeSeriesMetadata {

    /**
     * 获取设备属性时序数据元数据
     *
     * @param productId 型号ID
     * @return 元数据
     */
    static TimeSeriesMetadata properties(String productId) {
        return new DevicePropertiesTimeSeriesMetadata(productId);
    }

    static TimeSeriesMetadata properties(String productId, List<PropertyMetadata> properties) {
        return new FixedPropertiesTimeSeriesMetadata(productId, properties);
    }


    /**
     * 获取设备事件时序数据元数据
     *
     * @param productId 设备型号ID
     * @param metadata  事件元数据
     * @return 时序元数据
     */
    static TimeSeriesMetadata event(String productId, EventMetadata metadata) {
        return new DeviceEventTimeSeriesMetadata(productId, metadata);
    }

    /**
     * 获取设备日志时序数据元数据
     *
     * @param productId 设备型号ID
     * @return 时序元数据
     */
    static TimeSeriesMetadata log(String productId) {
        return new DeviceLogTimeSeriesMetadata(productId);
    }


}
