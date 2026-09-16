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
package org.jetlinks.community.network.mqtt.server;

import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttVersion;
import lombok.extern.slf4j.Slf4j;
import org.jctools.maps.NonBlockingHashMap;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.device.AuthenticationResponse;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.device.MqttAuthenticationRequest;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.reactor.mqtt.MqttAuth;
import org.jetlinks.reactor.mqtt.server.MqttAuthenticator;
import org.jetlinks.reactor.mqtt.server.ServerConnection;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * JetLinks MQTT 认证器适配器
 * <p>
 * 将 JetLinks 的设备协议认证逻辑包装成 reactor-mqtt 的 MqttAuthenticator。
 * 认证在连接建立时（accept 之前）执行，认证成功后创建 JetlinksMqttConnection 并设置认证结果。
 * </p>
 *
 * @author PengyuDeng
 * @since 2.11
 */
@Slf4j
public class JetlinksMqttAuthenticator implements MqttAuthenticator {

    /**
     * 存储 ServerConnection 到 JetlinksMqttConnection 的映射
     */
    private final Map<ServerConnection, JetlinksMqttConnection> connectionMap = new NonBlockingHashMap<>();

    private final DeviceRegistry registry;
    private final Transport transport;


    /**
     * 创建认证器
     *
     * @param registry            设备注册中心
     * @param transport           传输协议
     * @param protocolSupportMono 自定义协议支持（可选）
     */
    public JetlinksMqttAuthenticator(DeviceRegistry registry,
                                     Transport transport) {
        this.registry = registry;
        this.transport = transport;
    }

    @Override
    public Mono<MqttConnectReturnCode> authenticate(ServerConnection connection) {
        MqttAuth auth = connection.getAuth();
        String clientId = connection.getClientId();

        // 构建认证请求
        MqttAuthenticationRequest request = new MqttAuthenticationRequest(
            clientId,
            auth != null ? auth.getUsername() : null,
            auth != null ? auth.getPassword() : null,
            transport
        );

        MqttVersion version = connection.getVersion();

        return registry
            .getDevice(clientId)
            .flatMap(device -> device.authenticate(request))
            .flatMap(resp -> handleAuthResponse(connection, clientId, resp, version))
            // 认证结果为空，返回失败
            .defaultIfEmpty(getBadUsernameOrPasswordCode(version))
            .onErrorResume(err -> {
                log.error("MQTT连接认证[{}]失败", clientId, err);
                return Mono.just(getAuthErrorCode(version));
            });
    }

    private Mono<MqttConnectReturnCode> handleAuthResponse(ServerConnection connection,
                                                           String clientId,
                                                           AuthenticationResponse resp,
                                                           MqttVersion version) {
        if (!resp.isSuccess()) {
            log.debug("MQTT客户端[{}]认证失败: {}", clientId, resp.getMessage());
            return Mono.just(getBadUsernameOrPasswordCode(version));
        }

        // 认证响应可以自定义设备ID，如果没有则使用 mqtt 的 clientId
        String deviceId = StringUtils.hasText(resp.getDeviceId()) ? resp.getDeviceId() : clientId;

        return registry
            .getDevice(deviceId)
            .doOnNext(operator -> {
                // 创建 JetlinksMqttConnection 并设置认证结果
                JetlinksMqttConnection mqttConnection = new JetlinksMqttConnection(connection);
                mqttConnection.setDeviceOperator(operator);
                mqttConnection.setAuthResponse(resp);
                // 存储到 map 中，供后续获取
                connectionMap.put(connection, mqttConnection);
                // 连接关闭时从 map 中移除
                mqttConnection.onClose(conn -> connectionMap.remove(connection));
            })
            .map(operator -> MqttConnectReturnCode.CONNECTION_ACCEPTED)
            // 设备不存在
            .defaultIfEmpty(getIdentifierRejectedCode(version));
    }

    /**
     * 获取已认证的 JetlinksMqttConnection
     *
     * @param connection ServerConnection
     * @return JetlinksMqttConnection，如果不存在则返回 null
     */
    public JetlinksMqttConnection getConnection(ServerConnection connection) {
        return connectionMap.get(connection);
    }

    /**
     * 移除连接映射
     *
     * @param connection ServerConnection
     * @return 被移除的 JetlinksMqttConnection，如果不存在则返回 null
     */
    public JetlinksMqttConnection removeConnection(ServerConnection connection) {
        return connectionMap.remove(connection);
    }

    /**
     * 根据 MQTT 版本返回用户名或密码错误的返回码
     *
     * @param version MQTT 版本
     * @return 对应版本的返回码
     */
    private MqttConnectReturnCode getBadUsernameOrPasswordCode(MqttVersion version) {
        if (version == MqttVersion.MQTT_5) {
            return MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USERNAME_OR_PASSWORD;
        }
        // MQTT 3.1 / 3.1.1
        return MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
    }

    /**
     * 根据 MQTT 版本返回认证异常错误的返回码
     *
     * @param version MQTT 版本
     * @return 对应版本的返回码
     */
    private MqttConnectReturnCode getAuthErrorCode(MqttVersion version) {
        if (version == MqttVersion.MQTT_5) {
            return MqttConnectReturnCode.CONNECTION_REFUSED_BAD_AUTHENTICATION_METHOD;
        }
        return MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD;
    }

    /**
     * 根据 MQTT 版本返回客户端标识符被拒绝的返回码
     *
     * @param version MQTT 版本
     * @return 对应版本的返回码
     */
    private MqttConnectReturnCode getIdentifierRejectedCode(MqttVersion version) {
        if (version == MqttVersion.MQTT_5) {
            return MqttConnectReturnCode.CONNECTION_REFUSED_CLIENT_IDENTIFIER_NOT_VALID;
        }
        return MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED;
    }
}
