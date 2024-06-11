package org.jetlinks.community.network.tcp.parser;

import io.vertx.core.buffer.Buffer;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 不处理直接返回数据包
 *
 * @author zhouhao
 * @since 1.0
 */
public class DirectRecordParser implements PayloadParser {

    private final Sinks.Many<Buffer> sink = Reactors.createMany();

    @Override
    public void handle(Buffer buffer) {
        sink.emitNext(buffer, Reactors.emitFailureHandler());
    }

    @Override
    public Flux<Buffer> handlePayload() {
        return sink.asFlux();
    }

    @Override
    public void close() {
        sink.emitComplete(Reactors.emitFailureHandler());
    }
}
