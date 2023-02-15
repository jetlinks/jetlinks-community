package org.jetlinks.community.device.message.transparent;

import reactor.core.publisher.Mono;

import java.util.Map;

public interface TransparentMessageCodecProvider {

    String getProvider();

    Mono<TransparentMessageCodec> createCodec(Map<String,Object> configuration);

}
