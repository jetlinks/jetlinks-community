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
import reactor.core.publisher.Sinks;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class VertxTcpServer implements TcpServer {

    Collection<NetServer> tcpServers;

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
        if (this.tcpServers != null && !this.tcpServers.isEmpty()) {
            shutdown();
        }
        this.tcpServers = servers;

        for (NetServer tcpServer : this.tcpServers) {
            tcpServer.connectHandler(this::acceptTcpConnection);
        }

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
        if (null != tcpServers) {
            log.debug("close tcp server :[{}]", id);
            for (NetServer tcpServer : tcpServers) {
                execute(tcpServer::close);
            }
            tcpServers = null;
        }
    }

    @Override
    public boolean isAlive() {
        return tcpServers != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
