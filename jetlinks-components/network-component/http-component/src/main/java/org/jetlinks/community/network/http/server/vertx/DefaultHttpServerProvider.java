package org.jetlinks.community.network.http.server.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import lombok.Generated;
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
import org.jetlinks.core.metadata.types.ObjectType;
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

/**
 * 使用Vertx实现HTTP服务
 *
 * @author zhouhao
 * @since 1.0
 */
@Component
@Slf4j
@ConfigurationProperties(prefix = "jetlinks.network.http-server")
public class DefaultHttpServerProvider implements NetworkProvider<HttpServerConfig> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    @Getter
    @Setter
    private HttpServerOptions template = new HttpServerOptions();

    public DefaultHttpServerProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
        template.setHandle100ContinueAutomatically(true);
    }

    @Nonnull
    @Override
    @Generated
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Nonnull
    @Override
    public Mono<Network> createNetwork(@Nonnull HttpServerConfig config) {
        VertxHttpServer server = new VertxHttpServer(config);
        return initServer(server, config);
    }

    @Override
    public Mono<Network> reload(@Nonnull Network network, @Nonnull HttpServerConfig config) {
        VertxHttpServer server = ((VertxHttpServer) network);
        return initServer(server, config);
    }

    protected HttpServer createHttpServer(HttpServerOptions options) {
        return vertx.createHttpServer(options);
    }

    @Nullable
    @Override
    @Generated
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("id", "id", "", new StringType())
            .add("host", "本地地址", "", new StringType())
            .add("port", "本地端口", "", new IntType())
            .add("publicHost", "公网地址", "", new StringType())
            .add("publicPort", "公网端口", "", new IntType())
            .add("certId", "证书id", "", new StringType())
            .add("secure", "开启TSL", "", new BooleanType())
            .add("httpHeaders", "请求头", "", new ObjectType());
    }

    @Nonnull
    @Override
    public Mono<HttpServerConfig> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            HttpServerConfig config = FastBeanCopier.copy(properties.getConfigurations(), new HttpServerConfig());
            config.setId(properties.getId());
            config.validate();
            return Mono.just(config);
        })
            .as(LocaleUtils::transform);
    }

    private Mono<Network> initServer(VertxHttpServer server, HttpServerConfig config) {
        int numberOfInstance = Math.max(1, config.getInstance());
        List<HttpServer> instances = new ArrayList<>(numberOfInstance);
        return convert(config)
            .map(options -> {
                //利用多线程处理请求
                for (int i = 0; i < numberOfInstance; i++) {
                    instances.add(createHttpServer(options));
                }
                server.setBindAddress(new InetSocketAddress(config.getHost(), config.getPort()));
                server.setHttpServers(instances);
                for (HttpServer httpServer : instances) {
                    vertx.nettyEventLoopGroup()
                        .execute(()->{
                            httpServer.listen(result -> {
                                if (result.succeeded()) {
                                    log.debug("startup http server on [{}]", server.getBindAddress());
                                } else {
                                    server.setLastError(result.cause().getMessage());
                                    log.warn("startup http server on [{}] failed", server.getBindAddress(), result.cause());
                                }
                            });
                        });
                }
                return server;
            });
    }


    private Mono<HttpServerOptions> convert(HttpServerConfig config) {
        HttpServerOptions options = new HttpServerOptions(template);
        options.setHost(config.getHost());
        options.setPort(config.getPort());
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
