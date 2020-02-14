package org.jetlinks.community.network.tcp.parser;

import io.vertx.core.buffer.Buffer;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public class DirectRecordParser implements PayloadParser {

    EmitterProcessor<Buffer> processor = EmitterProcessor.create(false);


    @Override
    public void handle(Buffer buffer) {
        processor.onNext(buffer);
    }

    @Override
    public Flux<Buffer> handlePayload() {
        return processor.map(Function.identity());
    }

    @Override
    public void close() {
        processor.onComplete();
    }
}
