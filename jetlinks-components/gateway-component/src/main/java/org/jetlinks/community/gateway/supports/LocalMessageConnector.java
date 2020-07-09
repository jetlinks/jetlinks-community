package org.jetlinks.community.gateway.supports;

import org.jetlinks.community.gateway.MessageConnection;
import org.jetlinks.community.gateway.MessageConnector;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nonnull;
import java.util.function.Function;

class LocalMessageConnector implements MessageConnector {


    public LocalMessageConnector() {

    }

    @Nonnull
    @Override
    public String getId() {
        return "local";
    }

    @Override
    public String getName() {
        return "本地连接器";
    }

    private final EmitterProcessor<MessageConnection> processor = EmitterProcessor.create(false);

    private final FluxSink<MessageConnection> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    public LocalMessageConnection addConnection(String id, boolean shareCluster) {
        LocalMessageConnection connection = new LocalMessageConnection(id, shareCluster);
        sink.next(connection);
        return connection;
    }

    @Nonnull
    @Override
    public Flux<MessageConnection> onConnection() {
        return processor.map(Function.identity());
    }
}
