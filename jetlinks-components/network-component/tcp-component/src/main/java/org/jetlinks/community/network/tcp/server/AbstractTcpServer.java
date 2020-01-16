package org.jetlinks.community.network.tcp.server;

import org.jetlinks.community.network.tcp.client.TcpClient;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.function.Function;

/**
 * @author bsetfeng
 * @since 1.0
 **/
public abstract class AbstractTcpServer implements TcpServer{

    private EmitterProcessor<TcpClient> processor = EmitterProcessor.create(false);

    FluxSink<TcpClient> sink=processor.sink();

    protected void received(TcpClient tcpClient) {
       // if (processor.hasDownstreams()) {
        sink.next(tcpClient);
      //  }
    }

    @Override
    public Flux<TcpClient> handleConnection() {
        return processor
                .map(Function.identity());
    }
}
