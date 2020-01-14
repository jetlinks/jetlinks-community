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
public class DeviceEventIndex {
    public static ElasticIndex getDeviceEventIndex(String productId, String eventId) {
        return ElasticIndex.createDefaultIndex(
                () -> "event_".concat(productId).concat("_").concat(eventId),
                () -> "_doc");
    }

    public static ElasticIndex getDevicePropertiesIndex(String productId) {
        return ElasticIndex.createDefaultIndex(
            () -> "properties_".concat(productId),
            () -> "_doc");
    }
}
