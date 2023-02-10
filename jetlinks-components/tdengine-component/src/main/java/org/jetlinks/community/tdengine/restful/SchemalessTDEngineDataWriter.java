package org.jetlinks.community.tdengine.restful;

import io.netty.buffer.ByteBufAllocator;
import lombok.AllArgsConstructor;
import org.jetlinks.community.tdengine.TDEngineDataWriter;
import org.jetlinks.community.tdengine.TDengineProperties;
import org.jetlinks.community.buffer.BufferSettings;
import org.jetlinks.community.buffer.PersistenceBuffer;
import org.jetlinks.community.tdengine.Point;
import org.jetlinks.community.tdengine.TDEngineUtils;
import org.jetlinks.community.utils.ErrorUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class SchemalessTDEngineDataWriter implements TDEngineDataWriter, Disposable {
    private final WebClient client;

    private final String database;

    private final DataBufferFactory factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
    private final PersistenceBuffer<String> buffer;

    public SchemalessTDEngineDataWriter(WebClient client, String database, TDengineProperties.Buffer buffer) {
        this.client = client;
        this.database = database;
        if (buffer.isEnabled()) {
            this.buffer = new PersistenceBuffer<String>(
                BufferSettings.create("tdengine-writer.queue", buffer),
                null,
                list -> writeNow(list).thenReturn(false))
                .name("tdengine")
                .parallelism(buffer.getParallelism())
                .retryWhenError(e -> ErrorUtils.hasException(e, WebClientException.class)
                    || ErrorUtils.hasException(e, IOException.class));

            this.buffer.start();
        } else {
            this.buffer = null;
        }
    }

    @Override
    public void dispose() {
        if (null != buffer) {
            buffer.dispose();
        }
    }

    @Override
    public Mono<Void> write(Point point) {
        if (buffer == null) {
            return writeNow(Flux.just(convertToLine(point)));
        }
        buffer.write(convertToLine(point));

        return Mono.empty();
    }

    @Override
    public Mono<Void> write(Flux<Point> points) {
        return writeNow(points.map(this::convertToLine));
    }

    private static final byte[] newLine = "\n".getBytes();

    private Mono<Void> writeNow(Flux<String> lines) {

        return client
            .post()
            .uri(builder -> builder
                .path("/influxdb/v1/write")
                .queryParam("db", database)
                .build())
            .body(lines
                      .map(str -> {
                          byte[] data = str.getBytes();
                          return factory
                              .allocateBuffer(data.length + newLine.length)
                              .write(data)
                              .write(newLine);
                      })
                , DataBuffer.class)
            .exchangeToMono(TDEngineUtils::checkExecuteResult)
            .then();


    }

    private String convertToLine(Point point) {
        return org.influxdb.dto.Point.measurement(point.getMetric())
                                     .tag((Map) point.getTags())
                                     .fields(point.getValues())
                                     .time(point.getTimestamp(), TimeUnit.MILLISECONDS)
                                     .build()
                                     .lineProtocol();
    }
}
