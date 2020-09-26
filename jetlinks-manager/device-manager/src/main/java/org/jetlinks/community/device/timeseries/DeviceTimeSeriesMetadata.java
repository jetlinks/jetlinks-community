package org.jetlinks.community.device.timeseries;

import org.jetlinks.core.metadata.EventMetadata;
import org.jetlinks.community.timeseries.TimeSeriesMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;

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
