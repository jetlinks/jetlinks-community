package org.jetlinks.community.network.tcp.server;

import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.client.VertxTcpClient;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Slf4j
public class VertxTcpServer implements TcpServer {

    @Getter
    private final String id;
    private final EmitterProcessor<TcpClient> processor = EmitterProcessor.create(false);
    private final FluxSink<TcpClient> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    Collection<NetServer> tcpServers;
    private Supplier<PayloadParser> parserSupplier;
    @Setter
    private long keepAliveTimeout = Duration.ofMinutes(10).toMillis();

    public VertxTcpServer(String id) {
        this.id = id;
    }

    @Override
    public Flux<TcpClient> handleConnection() {
        return processor
            .map(Function.identity());
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close tcp server error", e);
        }
    }

    public void setParserSupplier(Supplier<PayloadParser> parserSupplier) {
        this.parserSupplier = parserSupplier;
    }

    /**
     * 为每个NetServer添加connectHandler
     *
     * @param servers 创建的所有NetServer
     */
    public void setServer(Collection<NetServer> servers) {
        if (this.tcpServers != null && !this.tcpServers.isEmpty()) {
            shutdown();
        }
        this.tcpServers = servers;

        for (NetServer tcpServer : this.tcpServers) {
            tcpServer.connectHandler(this::acceptTcpConnection);
        }

    }

    /**
     * TCP连接处理逻辑
     *
     * @param socket socket
     */
    protected void acceptTcpConnection(NetSocket socket) {
        if (!processor.hasDownstreams()) {
            log.warn("not handler for tcp client[{}]", socket.remoteAddress());
            socket.close();
            return;
        }
        // 客户端连接处理
        VertxTcpClient client = new VertxTcpClient(id + "_" + socket.remoteAddress(), true);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);
        try {
            // TCP异常和关闭处理
            socket.exceptionHandler(err -> {
                log.error("tcp server client [{}] error", socket.remoteAddress(), err);
            }).closeHandler((nil) -> {
                log.debug("tcp server client [{}] closed", socket.remoteAddress());
                client.shutdown();
            });
            // 这个地方是在TCP服务初始化的时候设置的 parserSupplier
            // set方法 org.jetlinks.community.network.tcp.server.VertxTcpServer.setParserSupplier
            // 调用坐标 org.jetlinks.community.network.tcp.server.TcpServerProvider.initTcpServer
            client.setRecordParser(parserSupplier.get());
            client.setSocket(socket);
            // client放进了发射器
            sink.next(client);
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
