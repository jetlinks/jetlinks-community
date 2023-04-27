package org.jetlinks.community.network.manager.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
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

    public static DeviceGatewayDetail of(DeviceGatewayEntity entity) {
        return FastBeanCopier.copy(entity, new DeviceGatewayDetail());
    }

    public Mono<DeviceGatewayDetail> with(ProtocolSupport protocol) {
        this.protocolDetail = new ProtocolDetail(protocol.getId(), protocol.getName(), protocol.getDescription(), null);

        return TransportDetail
            .of(protocol, Transport.of(transport))
            .doOnNext(this::setTransportDetail)
            .thenReturn(this)
            ;
    }

    public DeviceGatewayDetail with(ChannelInfo channelInfo) {
        this.channelInfo = channelInfo;
        return this;
    }


}
