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
package org.jetlinks.community.device.web.protocol;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.jetlinks.community.protocol.ProtocolFeature;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.route.Route;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import reactor.core.publisher.Mono;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Generated
public class TransportDetail {
    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "其他设置")
    private List<ProtocolFeature> features;

    @Schema(description = "路由信息")
    private List<Route> routes;

    @Schema(description = "文档信息")
    private String document;

    @Schema(description = "默认物模型")
    private String metadata;

    public static Mono<TransportDetail> of(ProtocolSupport support, Transport transport) {
        return Mono
            .zip(
                support
                    //T1: 路由信息
                    .getRoutes(transport)
                    .collectList(),
                support
                    //T2: 协议特性
                    .getFeatures(transport)
                    .map(ProtocolFeature::of)
                    .collectList(),
                support
                    //T3: 默认物模型
                    .getDefaultMetadata(transport)
                    .flatMap(JetLinksDeviceMetadataCodec.getInstance()::encode)
                    .defaultIfEmpty("")
            )
            .map(tp3 -> new TransportDetail(
                transport.getId(),
                transport.getName(),
                tp3.getT2(),
                tp3.getT1(),
                support.getDocument(transport),
                tp3.getT3()));

    }
}