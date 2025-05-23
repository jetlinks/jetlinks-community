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
package org.jetlinks.community.gateway.supports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.community.ValueObject;

import jakarta.validation.constraints.NotBlank;
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
