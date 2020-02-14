package org.jetlinks.community.network.tcp.client;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.tcp.TcpMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Slf4j
public abstract class AbstractTcpClient implements TcpClient {

    private EmitterProcessor<TcpMessage> processor = EmitterProcessor.create(false);

    protected void received(TcpMessage message) {
        if (processor.getPending() > processor.getBufferSize() / 2) {
            log.warn("not handler,drop tcp message:{}", message.getPayload().toString(StandardCharsets.UTF_8));
            return;
        }
        processor.onNext(message);
    }

    @Override
    public Flux<TcpMessage> subscribe() {
        return processor
                .map(Function.identity());
    }

}
