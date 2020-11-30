package org.jetlinks.community.network.tcp.client;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.Values;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.security.VertxKeyCertTrustOptions;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

@Component
@Slf4j
public class VertxTcpClientProvider implements NetworkProvider<TcpClientProperties> {

    private final CertificateManager certificateManager;

    private final PayloadParserBuilder payloadParserBuilder;

    private final Vertx vertx;

    public VertxTcpClientProvider(CertificateManager certificateManager, Vertx vertx, PayloadParserBuilder payloadParserBuilder) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
        this.payloadParserBuilder = payloadParserBuilder;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.TCP_CLIENT;
    }

    @Nonnull
    @Override
    public VertxTcpClient createNetwork(@Nonnull TcpClientProperties properties) {
        VertxTcpClient client = new VertxTcpClient(properties.getId(),false);

        initClient(client, properties);

        return client;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull TcpClientProperties properties) {
        initClient(((VertxTcpClient) network), properties);
    }

    public void initClient(VertxTcpClient client, TcpClientProperties properties) {
        NetClient netClient = vertx.createNetClient(properties.getOptions());
        client.setClient(netClient);
        client.setKeepAliveTimeoutMs(properties.getLong("keepAliveTimeout").orElse(Duration.ofMinutes(10).toMillis()));
        netClient.connect(properties.getPort(), properties.getHost(), result -> {
            if (result.succeeded()) {
                log.debug("connect tcp [{}:{}] success", properties.getHost(), properties.getPort());
                client.setRecordParser(payloadParserBuilder.build(properties.getParserType(), properties));
                client.setSocket(result.result());
            } else {
                log.error("connect tcp [{}:{}] error", properties.getHost(), properties.getPort(),result.cause());
            }
        });
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        // TODO: 2019/12/19
        return null;
    }

    @Nonnull
    @Override
    public Mono<TcpClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            TcpClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new TcpClientProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new NetClientOptions());
            }
            if (config.isSsl()) {
                config.getOptions().setSsl(true);
                return certificateManager.getCertificate(config.getCertId())
                        .map(VertxKeyCertTrustOptions::new)
                        .doOnNext(config.getOptions()::setKeyCertOptions)
                        .doOnNext(config.getOptions()::setTrustOptions)
                        .thenReturn(config);
            }
            return Mono.just(config);
        });
    }
}
