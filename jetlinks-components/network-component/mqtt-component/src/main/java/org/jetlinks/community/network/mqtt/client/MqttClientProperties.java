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
package org.jetlinks.community.network.mqtt.client;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.network.AbstractClientNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;

import java.util.Objects;

/**
 * MQTT Client 配置信息
 *
 * @author PengyuDeng
 * @since 2.11
 */
@Getter
@Setter
public class MqttClientProperties extends AbstractClientNetworkConfig {

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 证书ID
     */
    private String certId;

    //最大消息长度
    private int maxMessageSize = 0XFFFFFF;

    //共享订阅前缀
    private String topicPrefix;

    /**
     * TSL
     */
    private boolean secure;

    @Override
    public NetworkTransport getTransport() {
        return NetworkTransport.TCP;
    }

    @Override
    public String getSchema() {
        return isSecure()?"mqtts":"mqtt";
    }

    /**
     * 比较两个配置是否相同（用于判断是否需要重启连接）
     *
     * @param other 另一个配置
     * @return 配置是否相同
     */
    public boolean isSameConfig(MqttClientProperties other) {
        if (other == null) {
            return false;
        }
        return Objects.equals(this.remoteHost, other.remoteHost)
            && this.remotePort == other.remotePort
            && Objects.equals(this.clientId, other.clientId)
            && Objects.equals(this.username, other.username)
            && Objects.equals(this.password, other.password)
            && Objects.equals(this.certId, other.certId)
            && this.maxMessageSize == other.maxMessageSize
            && Objects.equals(this.topicPrefix, other.topicPrefix)
            && this.secure == other.secure;
    }
}
