package org.jetlinks.community.network.mqtt.server.vertx;

import io.vertx.core.Vertx;
import io.vertx.mqtt.MqttServer;
import io.vertx.mqtt.MqttServerOptions;
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
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@ConfigurationProperties(prefix = "jetlinks.network.mqtt-server")
public class DefaultVertxMqttServerProvider implements NetworkProvider<VertxMqttServerProperties> {

    private final CertificateManager certificateManager;
    private final Vertx vertx;

    @Getter
    @Setter
    private MqttServerOptions template = new MqttServerOptions();

    public DefaultVertxMqttServerProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
        template.setTcpKeepAlive(true);
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    @Nonnull
    @Override
    public Mono<Network> createNetwork(@Nonnull VertxMqttServerProperties properties) {
        VertxMqttServer server = new VertxMqttServer(properties.getId());
        return initServer(server, properties);
    }

    private Mono<Network> initServer(VertxMqttServer server, VertxMqttServerProperties properties) {
        int numberOfInstance = Math.max(1, properties.getInstance());
        return convert(properties)
            .map(options -> {
                List<MqttServer> instances = new ArrayList<>(numberOfInstance);
                for (int i = 0; i < numberOfInstance; i++) {
                    MqttServer mqttServer = MqttServer.create(vertx, options);
                    instances.add(mqttServer);
                }
                server.setBind(new InetSocketAddress(options.getHost(), options.getPort()));
                server.setMqttServer(instances);
                for (MqttServer instance : instances) {
                   vertx.nettyEventLoopGroup()
                       .execute(()->{
                           instance.listen(result -> {
                               if (result.succeeded()) {
                                   log.debug("startup mqtt server [{}] on port :{} ", properties.getId(), result
                                       .result()
                                       .actualPort());
                               } else {
                                   server.setLastError(result.cause().getMessage());
                                   log.warn("startup mqtt server [{}] error ", properties.getId(), result.cause());
                               }
                           });
                       });
                }
                return server;
            });

    }

    @Override
    public Mono<Network> reload(@Nonnull Network network, @Nonnull VertxMqttServerProperties properties) {
        log.debug("reload mqtt server[{}]", properties.getId());
        return initServer((VertxMqttServer) network, properties);
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
            .add("secure", "开启TSL", "", new BooleanType())
            .add("maxMessageSize", "最大消息长度", "", new StringType());
    }

    @Nonnull
    @Override
    public Mono<VertxMqttServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            VertxMqttServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new VertxMqttServerProperties());
            config.setId(properties.getId());
            config.validate();
            return Mono.just(config);
        })
            .as(LocaleUtils::transform);
    }

    private Mono<MqttServerOptions> convert(VertxMqttServerProperties properties) {
        //MqttServerOptions options = FastBeanCopier.copy(properties, new MqttServerOptions());
        MqttServerOptions options = new MqttServerOptions(template);
        options.setPort(properties.getPort());
        options.setHost(properties.getHost());
        options.setMaxMessageSize(properties.getMaxMessageSize());
        if (properties.isSecure()) {
            options.setSsl(true);
            return certificateManager
                .getCertificate(properties.getCertId())
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
