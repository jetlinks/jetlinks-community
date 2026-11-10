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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.Certificate;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.reactor.mqtt.client.MqttClient;
import org.jetlinks.reactor.mqtt.client.ReconnectStrategy;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

/**
 * MQTT Client 网络组件提供商
 *
 * @author PengyuDeng
 * @since 2.11
 */
@Component
@Slf4j
@ConfigurationProperties(prefix = "jetlinks.network.mqtt-client")
public class JetlinksMqttClientProvider implements NetworkProvider<MqttClientProperties> {

    private final CertificateManager certificateManager;

    @Getter
    @Setter
    private int keepAliveInterval = 180;

    public JetlinksMqttClientProvider(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_CLIENT;
    }

    @Nonnull
    @Override
    public Mono<Network> createNetwork(@Nonnull MqttClientProperties properties) {
        DefaultJetlinksMqttClient mqttClient = new DefaultJetlinksMqttClient(properties.getId());
        return doStart(mqttClient, properties);
    }

    @Override
    public Mono<Network> reload(@Nonnull Network network, @Nonnull MqttClientProperties properties) {
        DefaultJetlinksMqttClient mqttClient = ((DefaultJetlinksMqttClient) network);
        if (mqttClient.isSameConfig(properties)) {
            return Mono.just(network);
        }
        return mqttClient.shutdownAsync()
                         .then(doStart(mqttClient, properties));
    }

    public Mono<Network> doStart(DefaultJetlinksMqttClient mqttClient, MqttClientProperties properties) {
        mqttClient.setMqttClientProperties(properties);
        return initClient(properties)
            .flatMap(client -> {
                mqttClient.setTopicPrefix(properties.getTopicPrefix());
                return client.connect()
                             .timeout(Duration.ofSeconds(30))
                             .doOnSuccess(connection -> {
                                 mqttClient.setConnection(connection);
                                 log.debug("connect mqtt [{}] success", properties.getId());
                             })
                             .doOnError(err -> {
                                 mqttClient.setConnection(null);
                                 log.warn("connect mqtt [{}@{}:{}] error",
                                          properties.getClientId(),
                                          properties.getRemoteHost(),
                                          properties.getRemotePort(),
                                          err);
                             })
                             .thenReturn(mqttClient);
            });
    }

    private Mono<MqttClient> initClient(MqttClientProperties config) {
        MqttClient client = MqttClient.create()
                                      .host(config.getRemoteHost())
                                      .port(config.getRemotePort())
                                      .clientId(config.getClientId())
                                      .keepAlive(keepAliveInterval)
                                      .maxMessageSize(config.getMaxMessageSize())
                                      .reconnectStrategy(ReconnectStrategy.exponentialBackoff(
                                          Duration.ofSeconds(1),
                                          Duration.ofMinutes(5)
                                      ))
                                      .autoResubscribe(true);

        if (config.getUsername() != null) {
            client.auth(config.getUsername(), config.getPassword());
        }

        if (config.isSecure()) {
            return certificateManager
                .getCertificate(config.getCertId())
                .flatMap(this::createSslContext)
                .map(sslContext -> {
                    client.ssl(sslContext);
                    return client;
                });
        }

        return Mono.just(client);
    }

    private Mono<SslContext> createSslContext(Certificate certificate) {
        return Mono.fromCallable(() -> {
            SslContextBuilder builder = SslContextBuilder.forClient();
            if (certificate.getKeyManagerFactory() != null) {
                builder.keyManager(certificate.getKeyManagerFactory());
            }
            if (certificate.getTrustManagerFactory() != null) {
                builder.trustManager(certificate.getTrustManagerFactory());
            }
            return builder.build();
        });
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("remoteHost", "远程地址", "", new StringType())
            .add("remotePort", "远程端口", "", new IntType())
            .add("certId", "证书ID", "", new StringType())
            .add("secure", "开启TSL", "", new BooleanType())
            .add("clientId", "客户端ID", "", new StringType())
            .add("username", "用户名", "", new StringType())
            .add("password", "密码", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<MqttClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono
            .defer(() -> {
                MqttClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new MqttClientProperties());
                config.setId(properties.getId());
                config.validate();
                return Mono.just(config);
            })
            .as(LocaleUtils::transform);
    }

    @Override
    public boolean isReusable() {
        return true;
    }
}
