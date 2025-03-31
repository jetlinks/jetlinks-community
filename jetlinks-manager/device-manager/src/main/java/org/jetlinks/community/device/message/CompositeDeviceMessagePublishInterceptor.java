package org.jetlinks.community.device.message;

import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.DeviceMessage;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeDeviceMessagePublishInterceptor implements DeviceMessagePublishInterceptor {

    private final List<DeviceMessagePublishInterceptor> listeners = new CopyOnWriteArrayList<>();

    public void addInterceptor(DeviceMessagePublishInterceptor listener) {
        listeners.add(listener);
        listeners.sort(Comparator.comparingInt(DeviceMessagePublishInterceptor::getOrder));
    }

    @Override
    public Mono<DeviceMessage> beforePublish(DeviceOperator device, DeviceMessage message) {
        Mono<DeviceMessage> mono = null;
        for (DeviceMessagePublishInterceptor listener : listeners) {
            if (!listener.isSupport(message)) {
                continue;
            }
            if (mono == null) {
                mono = listener.beforePublish(device, message);
            } else {
                mono = mono.flatMap(msg -> listener.beforePublish(device, msg).defaultIfEmpty(msg));
            }
        }
        if (mono == null) {
            return Mono.just(message);
        }
        return mono;
    }
}
