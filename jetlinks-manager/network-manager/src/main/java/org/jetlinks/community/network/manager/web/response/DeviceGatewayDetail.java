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
package org.jetlinks.community.network.manager.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.channel.ChannelProvider;
import org.jetlinks.community.network.manager.entity.DeviceGatewayEntity;
import org.jetlinks.community.network.manager.enums.DeviceGatewayState;
import org.jetlinks.community.protocol.ProtocolDetail;
import org.jetlinks.community.protocol.TransportDetail;
import reactor.core.publisher.Mono;

import java.util.Map;

@Getter
@Setter
public class DeviceGatewayDetail {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    /**
     * @see DeviceGatewayProvider#getId()
     */
    @Schema(description = "接入类型,如: modbus,mqtt-server")
    private String provider;

    /**
     * @see ProtocolSupport#getId()
     */
    @Schema(description = "消息协议")
    private String protocol;

    /**
     * @see Transport#getId()
     */
    @Schema(description = "传输协议,如:TCP,UDP")
    private String transport;

    /**
     * @see ChannelProvider#getChannel()
     */
    @Schema(description = "通道")
    private String channel;

    @Schema(description = "通道ID")
    private String channelId;

    @Schema(description = "状态")
    private DeviceGatewayState state;

    @Schema(description = "通道信息")
    private ChannelInfo channelInfo;

    @Schema(description = "消息协议详情")
    private ProtocolDetail protocolDetail;

    @Schema(description = "传输协议详情")
    private TransportDetail transportDetail;

    @Schema(description = "配置信息(根据类型不同而不同)")
    private Map<String, Object> configuration;

    public static DeviceGatewayDetail of(DeviceGatewayEntity entity) {
        return FastBeanCopier.copy(entity, new DeviceGatewayDetail());
    }

    public Mono<DeviceGatewayDetail> with(ProtocolSupport protocol) {
        return Mono
            .zip(
                TransportDetail
                    .of(protocol, Transport.of(transport))
                    .doOnNext(this::setTransportDetail),
                ProtocolDetail
                    .of(protocol)
                    .doOnNext(this::setProtocolDetail)
            )
            .as(LocaleUtils::transform)
            .thenReturn(this);
    }

    public DeviceGatewayDetail with(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
        return this;
    }


}
