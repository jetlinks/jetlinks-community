package org.jetlinks.community.device.web.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetlinks.core.ProtocolSupport;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Generated
@NoArgsConstructor
public class ProtocolDetail {
    @Schema(description = "协议ID")
    private String id;

    @Schema(description = "协议名称")
    private String name;

    @Schema(description = "协议说明")
    private String description;

    private List<TransportDetail> transports;

    public static Mono<ProtocolDetail> of(ProtocolSupport support) {

        return support
            .getSupportedTransport()
            .flatMap(trans -> TransportDetail.of(support, trans))
            .collectList()
            .map(details -> new ProtocolDetail(support.getId(), support.getName(),support.getDescription(), details));
    }
}

