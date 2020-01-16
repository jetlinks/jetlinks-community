package org.jetlinks.community.network.tcp.client;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.tcp.TcpMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Slf4j
public abstract class AbstractTcpClient implements TcpClient {

    private EmitterProcessor<TcpMessage> processor = EmitterProcessor.create(false);

    private FluxSink<TcpMessage> sink = processor.sink();

    protected void received(TcpMessage message) {
        if (processor.getPending() > processor.getBufferSize() / 2) {
            log.warn("not handler,drop tcp message:{}", message.getPayload().toString(StandardCharsets.UTF_8));
            return;
        }
        sink.next(message);
    }

    @Override
    public Flux<TcpMessage> subscribe() {
        return processor
            .map(Function.identity());
    }

}
