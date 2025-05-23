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
import org.jetlinks.community.network.AbstractClientNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;

/**
 * MQTT Client 配置信息
 *
 * @author zhouhao
 * @since 1.0
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
}
