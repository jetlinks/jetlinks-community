package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import org.jetlinks.core.Values;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public abstract class VertxPayloadParserBuilder implements PayloadParserBuilderStrategy {
    @Override
    public abstract PayloadParserType getType();

    protected abstract RecordParser createParser(Values config);

    @Override
    public PayloadParser build(Values config) {
        return new RecordPayloadParser(createParser(config));
    }

    class RecordPayloadParser implements PayloadParser {
        RecordParser recordParser;
        EmitterProcessor<Buffer> processor = EmitterProcessor.create(false);

        public RecordPayloadParser(RecordParser recordParser) {
            this.recordParser = recordParser;
            this.recordParser.handler(buffer -> {
               // if (processor.hasDownstreams()) {
                    processor.onNext(buffer);
               // }
            });
        }

        @Override
        public void handle(Buffer buffer) {
            recordParser.handle(buffer);
        }

        @Override
        public Flux<Buffer> handlePayload() {
            return processor.map(Function.identity());
        }

        @Override
        public void close() {
            try {
                processor.dispose();
            } catch (Exception ignore) {

            }
        }
    }


}
