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

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.Certificate;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.reactor.mqtt.server.MqttServer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;

@Slf4j
@Component
@ConfigurationProperties(prefix = "jetlinks.network.mqtt-server")
public class JetlinksMqttServerProvider implements NetworkProvider<MqttServerProperties> {

    private final CertificateManager certificateManager;

    private final DeviceRegistry registry;

    public JetlinksMqttServerProvider(CertificateManager certificateManager, DeviceRegistry registry) {
        this.certificateManager = certificateManager;
        this.registry = registry;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    @Nonnull
    @Override
    public Mono<Network> createNetwork(@Nonnull MqttServerProperties properties) {
        DefaultJetlinksMqttServer server = new DefaultJetlinksMqttServer(properties.getId());
        return initServer(server, properties);
    }

    private Mono<Network> initServer(DefaultJetlinksMqttServer server, MqttServerProperties properties) {
        JetlinksMqttAuthenticator authenticator = new JetlinksMqttAuthenticator(registry, getTransport());
        return createJetlinksMqttServer(properties, authenticator)
            .flatMap(reactorServer -> {
                server.setBind(new InetSocketAddress(properties.getHost(), properties.getPort()));
                server.setReactorServer(reactorServer);
                server.setAuthenticator(authenticator);
                return server.start().thenReturn(server);
            });
    }

    private Mono<MqttServer> createJetlinksMqttServer(MqttServerProperties properties, JetlinksMqttAuthenticator authenticator) {
        MqttServer server = MqttServer.create()
                                      .host(properties.getHost())
                                      .port(properties.getPort())
                                      .maxMessageSize(properties.getMaxMessageSize())
                                      .workerCount(Math.max(1, properties.getInstance()))
                                      .autoAck(true)
                                      .tcpKeepAlive(true)
                                      .tcpNoDelay(true)
                                      .authenticator(authenticator);

        if (properties.isSecure()) {
            return certificateManager
                .getCertificate(properties.getCertId())
                .flatMap(this::createSslContext)
                .map(sslContext -> {
                    server.ssl(sslContext);
                    return server;
                });
        }

        return Mono.just(server);
    }

    private Mono<SslContext> createSslContext(Certificate certificate) {
        return Mono.fromCallable(() -> {
            SslContextBuilder builder = SslContextBuilder.forServer(certificate.getKeyManagerFactory());
            if (certificate.getTrustManagerFactory() != null) {
                builder.trustManager(certificate.getTrustManagerFactory());
            }
            return builder.build();
        });
    }

    @Override
    public Mono<Network> reload(@Nonnull Network network, @Nonnull MqttServerProperties properties) {
        log.debug("reload mqtt server[{}]", properties.getId());
        DefaultJetlinksMqttServer server = (DefaultJetlinksMqttServer) network;
        server.shutdown();
        return initServer(server, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("host", "本地地址", "", new StringType())
            .add("port", "本地端口", "", new IntType())
            .add("publicHost", "公网地址", "", new StringType())
            .add("publicPort", "公网端口", "", new IntType())
            .add("certId", "证书id", "", new StringType())
            .add("secure", "开启TSL", "", new BooleanType())
            .add("maxMessageSize", "最大消息长度", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<MqttServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            MqttServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new MqttServerProperties());
            config.setId(properties.getId());
            config.validate();
            return Mono.just(config);
        }).as(LocaleUtils::transform);
    }

    @Override
    public boolean isReusable() {
        return true;
    }

    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }
}
