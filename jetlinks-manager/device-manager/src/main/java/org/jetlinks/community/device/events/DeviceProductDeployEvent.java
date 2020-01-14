package org.jetlinks.community.device.events;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class DeviceProductDeployEvent {

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

    private String orgId;
}
