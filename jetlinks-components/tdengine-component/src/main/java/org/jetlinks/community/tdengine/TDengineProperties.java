package org.jetlinks.community.tdengine;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.buffer.BufferProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;

import javax.sql.DataSource;
import javax.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@ConfigurationProperties(prefix = "tdengine")
public class TDengineProperties {

    @NotBlank
    private String database;

    private Connector connector = Connector.restful;

    private RestfulConnector restful = new RestfulConnector();

    //缓冲配置
    private Buffer buffer = new Buffer();

    enum Connector {
        restful
    }

    @Getter
    @Setter
    public static class Buffer extends BufferProperties {
        private boolean enabled = true;

        public Buffer() {
            setFilePath("./data/tdengine-buffer");
            setSize(3000);
        }
    }

    @Getter
    @Setter
    public static class RestfulConnector {

        private List<URI> endpoints = new ArrayList<>(Collections.singletonList(URI.create("http://localhost:6041/")));

        private String username = "root";

        private String password = "taosdata";

        private int maxConnections = Runtime.getRuntime().availableProcessors() * 8;

        private Duration pendingAcquireTimeout = Duration.ofSeconds(10);
        private Duration evictInBackground = Duration.ofSeconds(60);

        private Duration connectionTimeout = Duration.ofSeconds(5);

        private Duration socketTimeout = Duration.ofSeconds(5);

        private DataSize maxInMemorySize = DataSize.ofMegabytes(10);

        public URI selectURI() {
            // TODO: 2021/6/2 更好的负载均衡方式
            return endpoints.get(ThreadLocalRandom.current().nextInt(endpoints.size()));
        }

        public WebClient createClient() {
            WebClient.Builder builder = WebClient.builder();
            URI endpoint = endpoints.get(0);

            if (endpoints.size() > 1) {
                builder = builder.filter((request, next) -> {
                    URI target = selectURI();
                    if (target.equals(endpoint)) {
                        return next.exchange(request);
                    }
                    URI uri = UriComponentsBuilder
                        .fromUri(request.url())
                        .host(target.getHost())
                        .port(target.getPort())
                        .build()
                        .toUri();
                    return next
                        .exchange(ClientRequest
                                      .from(request)
                                      .url(uri)
                                      .build());
                });
            }

            return builder
                .codecs(clientCodecConfigurer -> clientCodecConfigurer
                    .defaultCodecs()
                    .maxInMemorySize((int) maxInMemorySize.toBytes()))
                .defaultHeaders(headers -> {
                    if (StringUtils.hasText(username)) {
                        headers.setBasicAuth(username, password);
                    }
                })
                .baseUrl(endpoint.toString())
                .build();
        }
    }
}
