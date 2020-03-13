package org.jetlinks.community.device.web.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ProtocolDetail {
    private String id;

    private String name;

    private List<TransportDetail> transports;

    public static Mono<ProtocolDetail> of(ProtocolSupport support) {
        return support
            .getSupportedTransport()
            .flatMap(trans -> TransportDetail.of(support, trans))
            .collectList()
            .map(details -> new ProtocolDetail(support.getId(), support.getName(), details));
    }
}




