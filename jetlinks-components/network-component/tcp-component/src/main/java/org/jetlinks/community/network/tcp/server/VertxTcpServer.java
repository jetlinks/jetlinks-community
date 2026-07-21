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
package org.jetlinks.community.network.tcp.server;

import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.client.VertxTcpClient;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class VertxTcpServer implements TcpServer {

    private volatile Collection<NetServer> tcpServers;

    private final AtomicBoolean started = new AtomicBoolean();

    private Supplier<PayloadParser> parserSupplier;

    @Setter
    private long keepAliveTimeout = Duration.ofMinutes(10).toMillis();

    @Getter
    private final String id;


    private final Sinks.Many<TcpClient> sink = Reactors.createMany(Integer.MAX_VALUE,false);

    @Getter
    @Setter
    private String lastError;

    @Setter(AccessLevel.PACKAGE)
    private InetSocketAddress bind;

    public VertxTcpServer(String id) {
        this.id = id;
    }

    @Override
    public Flux<TcpClient> handleConnection() {
        return sink.asFlux();
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close tcp server error", e);
        }
    }

    @Override
    public InetSocketAddress getBindAddress() {
        return bind;
    }

    public void setParserSupplier(Supplier<PayloadParser> parserSupplier) {
        this.parserSupplier = parserSupplier;
    }

    public void setServer(Collection<NetServer> servers) {
        started.set(false);
        if (this.tcpServers != null && !this.tcpServers.isEmpty()) {
            shutdown();
        }
        this.tcpServers = servers;

        for (NetServer tcpServer : this.tcpServers) {
            tcpServer.connectHandler(this::acceptTcpConnection);
        }

    }

    void startupComplete() {
        started.set(true);
    }

    Mono<Void> shutdownAsync() {
        return Flux
            .fromIterable(clearServers())
            .flatMap(tcpServer -> Mono
                .fromCompletionStage(tcpServer.close().toCompletionStage())
                .onErrorResume(error -> {
                    log.warn("close tcp server error", error);
                    return Mono.empty();
                }))
            .then();
    }

    private synchronized Collection<NetServer> clearServers() {
        started.set(false);
        Collection<NetServer> servers = tcpServers;
        tcpServers = null;
        return servers == null ? Collections.emptyList() : servers;
    }

    protected void acceptTcpConnection(NetSocket socket) {
        if (sink.currentSubscriberCount() == 0) {
            log.warn("not handler for tcp client[{}]", socket.remoteAddress());
            socket.close();
            return;
        }
        VertxTcpClient client = new VertxTcpClient(id + "_" + socket.remoteAddress());
        client.setKeepAliveTimeoutMs(keepAliveTimeout);
        try {
            socket.exceptionHandler(err -> {
                log.error("tcp server client [{}] error", socket.remoteAddress(), err);
            });
            client.setRecordParser(parserSupplier.get());
            client.setSocket(socket);
            sink.emitNext(client, Reactors.emitFailureHandler());
            log.debug("accept tcp client [{}] connection", socket.remoteAddress());
        } catch (Exception e) {
            log.error("create tcp server client error", e);
            client.shutdown();
        }
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    @Override
    public void shutdown() {
        Collection<NetServer> servers = clearServers();
        if (!servers.isEmpty()) {
            log.debug("close tcp server :[{}]", id);
            for (NetServer tcpServer : servers) {
                execute(tcpServer::close);
            }
        }
    }

    @Override
    public boolean isAlive() {
        Collection<NetServer> servers = tcpServers;
        return started.get() &&
            servers != null &&
            !servers.isEmpty() &&
            servers.stream().allMatch(server -> server.actualPort() > 0);
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
