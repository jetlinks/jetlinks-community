package org.jetlinks.community.network.mqtt.client;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.network.*;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.security.VertxKeyCertTrustOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * MQTT Client 网络组件提供商
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
@ConfigurationProperties(prefix = "jetlinks.network.mqtt-client")
public class MqttClientProvider implements NetworkProvider<MqttClientProperties> {

    private final Vertx vertx;

    private final CertificateManager certificateManager;

    private final Environment environment;

    @Getter
    @Setter
    private MqttClientOptions template = new MqttClientOptions();


    public MqttClientProvider(CertificateManager certificateManager,
                              Vertx vertx,
                              Environment environment) {
        this.vertx = vertx;
        this.certificateManager = certificateManager;
        this.environment = environment;
        template.setTcpKeepAlive(true);
//        options.setReconnectAttempts(10);
        template.setAutoKeepAlive(true);
        template.setKeepAliveInterval(180);
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_CLIENT;
    }

    @Nonnull
    @Override
    public Mono<Network> createNetwork(@Nonnull MqttClientProperties properties) {
        VertxMqttClient mqttClient = new VertxMqttClient(properties.getId());
        return initMqttClient(mqttClient, properties);
    }

    @Override
    public Mono<Network> reload(@Nonnull Network network, @Nonnull MqttClientProperties properties) {
        VertxMqttClient mqttClient = ((VertxMqttClient) network);
        if (mqttClient.isLoading()) {
            return Mono.just(mqttClient);
        }
        return initMqttClient(mqttClient, properties);
    }

    public Mono<Network> initMqttClient(VertxMqttClient mqttClient, MqttClientProperties properties) {
        return convert(properties)
            .map(options -> {
                mqttClient.setTopicPrefix(properties.getTopicPrefix());
                mqttClient.setLoading(true);
                MqttClient client = MqttClient.create(vertx, options);
                mqttClient.setClient(client);
                client.connect(properties.getRemotePort(), properties.getRemoteHost(), result -> {
                    mqttClient.setLoading(false);
                    if (!result.succeeded()) {
                        log.warn("connect mqtt [{}@{}:{}] error",
                                 properties.getClientId(),
                                 properties.getRemoteHost(),
                                 properties.getRemotePort(),
                                 result.cause());
                    } else {
                        log.debug("connect mqtt [{}] success", properties.getId());
                    }
                });
                return mqttClient;
            });
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("remoteHost", "远程地址", "", new StringType())
            .add("remotePort", "远程地址", "", new IntType())
            .add("certId", "证书id", "", new StringType())
            .add("secure", "开启TSL", "", new BooleanType())
            .add("clientId", "客户端ID", "", new BooleanType())
            .add("username", "用户名", "", new BooleanType())
            .add("password", "密码", "", new BooleanType());
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


    private Mono<MqttClientOptions> convert(MqttClientProperties config) {
        MqttClientOptions options = FastBeanCopier.copy(config, new MqttClientOptions(template));

        String clientId = String.valueOf(config.getClientId());

        String username = config.getUsername();

        String password = config.getPassword();

        options.setClientId(clientId);
        options.setPassword(password);
        options.setUsername(username);

        if (config.isSecure()) {
            options.setSsl(true);
            return certificateManager
                .getCertificate(config.getCertId())
                .map(VertxKeyCertTrustOptions::new)
                .doOnNext(options::setKeyCertOptions)
                .doOnNext(options::setTrustOptions)
                .thenReturn(options);
        }
        return Mono.just(options);
    }

    @Override
    public boolean isReusable() {
        return true;
    }
}
