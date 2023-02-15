package org.jetlinks.community.device.message.transparent;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DirectDeviceMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransparentMessageCodec {

    Flux<DeviceMessage> decode(DirectDeviceMessage message);

    Mono<DirectDeviceMessage> encode(DeviceMessage message);

}
