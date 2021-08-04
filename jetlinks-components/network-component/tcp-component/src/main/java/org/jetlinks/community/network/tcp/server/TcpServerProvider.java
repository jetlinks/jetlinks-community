package org.jetlinks.community.network.tcp.server;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.security.VertxKeyCertTrustOptions;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilder;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * TCP服务提供商
 *
 * @author zhouhao
 */
@Component
@Slf4j
public class TcpServerProvider implements NetworkProvider<TcpServerProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    private final PayloadParserBuilder payloadParserBuilder;

    public TcpServerProvider(CertificateManager certificateManager, Vertx vertx, PayloadParserBuilder payloadParserBuilder) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
        this.payloadParserBuilder = payloadParserBuilder;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    @Nonnull
    @Override
    public VertxTcpServer createNetwork(@Nonnull TcpServerProperties properties) {

        VertxTcpServer tcpServer = new VertxTcpServer(properties.getId());
        initTcpServer(tcpServer, properties);

        return tcpServer;
    }

    /**
     * TCP服务初始化
     *
     * @param tcpServer  TCP服务
     * @param properties TCP配置
     */
    private void initTcpServer(VertxTcpServer tcpServer, TcpServerProperties properties) {
        int instance = Math.max(2, properties.getInstance());
        List<NetServer> instances = new ArrayList<>(instance);
        for (int i = 0; i < instance; i++) {
            instances.add(vertx.createNetServer(properties.getOptions()));
        }
        // 根据解析类型配置数据解析器
        payloadParserBuilder.build(properties.getParserType(), properties);
        tcpServer.setParserSupplier(() -> payloadParserBuilder.build(properties.getParserType(), properties));
        tcpServer.setServer(instances);
        tcpServer.setKeepAliveTimeout(properties.getLong("keepAliveTimeout", Duration.ofMinutes(10).toMillis()));
        // 针对JVM做的多路复用优化
        // 多个server listen同一个端口，每个client连接的时候vertx会分配
        // 一个connection只能在一个server中处理
        for (NetServer netServer : instances) {
            netServer.listen(properties.createSocketAddress(), result -> {
                if (result.succeeded()) {
                    log.info("tcp server startup on {}", result.result().actualPort());
                } else {
                    log.error("startup tcp server error", result.cause());
                }
            });
        }
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull TcpServerProperties properties) {
        VertxTcpServer tcpServer = ((VertxTcpServer) network);
        tcpServer.shutdown();
        initTcpServer(tcpServer, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<TcpServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            TcpServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new TcpServerProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new NetServerOptions());
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
