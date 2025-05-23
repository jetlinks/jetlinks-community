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
package org.jetlinks.community.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.message.codec.Transport;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Collections;
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

    public static Mono<ProtocolDetail> of(ProtocolSupport support, String transport) {
        if (!StringUtils.hasText(transport)){
            return of(support);
        }
        return getTransDetail(support, Transport.of(transport))
            .map(detail -> new ProtocolDetail(support.getId(), support.getName(), support.getDescription(), Collections.singletonList(detail)));
    }

    public static Mono<ProtocolDetail> of(ProtocolSupport support) {
        return support
            .getSupportedTransport()
            .flatMap(trans -> getTransDetail(support, trans))
            .collectList()
            .map(details -> new ProtocolDetail(support.getId(), support.getName(), support.getDescription(), details));
    }

    private static Mono<TransportDetail> getTransDetail(ProtocolSupport support, Transport transport) {
        return TransportDetail.of(support, transport);
    }
}




