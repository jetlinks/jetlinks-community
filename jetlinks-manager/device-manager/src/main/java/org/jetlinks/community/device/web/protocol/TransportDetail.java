package org.jetlinks.community.device.web.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.message.codec.Transport;
import reactor.core.publisher.Mono;


@Getter
@Setter
@AllArgsConstructor
public class TransportDetail {
    private String id;

    private String name;

    public static Mono<TransportDetail> of(ProtocolSupport support, Transport transport) {
        return Mono.just(new TransportDetail(transport.getId(), transport.getName()));
    }
}