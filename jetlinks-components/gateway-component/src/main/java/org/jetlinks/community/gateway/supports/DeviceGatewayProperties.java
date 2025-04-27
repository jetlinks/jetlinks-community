package org.jetlinks.community.gateway.supports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.community.ValueObject;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Generated
public class DeviceGatewayProperties implements ValueObject {

    private String id;

    private String name;

    private String description;

    /**
     * 设备接入网关提供商标识
     *
     * @see DeviceGatewayProvider#getId()
     */
    @NotBlank
    private String provider;

    @Schema(description = "接入通道ID,如网络组件ID")
    @NotBlank
    private String channelId;

    /**
     * @see ProtocolSupport#getId()
     */
    @Schema(description = "接入使用的消息协议")
    private String protocol;

    /**
     * 通信协议
     *
     * @see org.jetlinks.core.message.codec.DefaultTransport
     */
    private String transport;

    /**
     * 网关配置信息,由{@link this#provider}决定
     */
    private Map<String, Object> configuration = new HashMap<>();

    /**
     * 是否启用
     */
    private boolean enabled = true;

    @Override
    public Map<String, Object> values() {
        return configuration;
    }
}
