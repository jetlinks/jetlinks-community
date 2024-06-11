package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.ValueObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.function.Supplier;

public abstract class VertxPayloadParserBuilder implements PayloadParserBuilderStrategy {
    @Override
    public abstract PayloadParserType getType();

    protected abstract Supplier<RecordParser> createParser(ValueObject config);

    @Override
    public Supplier<PayloadParser> buildLazy(ValueObject config) {
        Supplier<RecordParser> parser = createParser(config);
        return () -> new RecordPayloadParser(parser);
    }

    static class RecordPayloadParser implements PayloadParser {
        private final Supplier<RecordParser> recordParserSupplier;
        private final Sinks.Many<Buffer> sink = Reactors.createMany();

        private RecordParser recordParser;

        public RecordPayloadParser(Supplier<RecordParser> recordParserSupplier) {
            this.recordParserSupplier = recordParserSupplier;
            reset();
        }

        @Override
        public void handle(Buffer buffer) {
            recordParser.handle(buffer);
        }

        @Override
        public Flux<Buffer> handlePayload() {
            return sink.asFlux();
        }

        @Override
        public void close() {
            sink.emitComplete(Reactors.emitFailureHandler());
        }

        @Override
        public void reset() {
            this.recordParser = recordParserSupplier.get();
            this.recordParser.handler(payload -> {
                sink.emitNext(payload, Reactors.emitFailureHandler());
            });
        }
    }

}
