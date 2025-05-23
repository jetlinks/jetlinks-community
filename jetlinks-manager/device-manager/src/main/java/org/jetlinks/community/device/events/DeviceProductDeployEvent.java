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
