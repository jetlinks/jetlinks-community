package org.jetlinks.community.device.events;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.event.DefaultAsyncEvent;
import org.jetlinks.community.gateway.supports.DeviceGatewayProvider;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
@Generated
public class DeviceProductDeployEvent extends DefaultAsyncEvent {

    private String id;

    private String name;

    private String projectId;

    private String projectName;

    private String describe;

    private String classifiedId;

    private String messageProtocol;

    private String metadata;

    private String transportProtocol;

    private String networkWay;

    private String deviceType;

    private Map<String, Object> configuration;

    private Byte state;

    private Long createTime;

    @Deprecated
    private String orgId;

    @Schema(description = "设备接入方式ID")
    private String accessId;

    /**
     * @see DeviceGatewayProvider#getId()
     */
    @Schema(description = "设备接入方式")
    private String accessProvider;

}
