package org.jetlinks.community.device.events.handler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.elastic.search.index.ElasticIndex;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public enum DeviceIndexProvider implements ElasticIndex {

    DEVICE_PROPERTIES("device_properties", "_doc"),
    DEVICE_OPERATION("device_operation", "_doc");

    private String index;

    private String type;
}
