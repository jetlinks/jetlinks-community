package org.jetlinks.community.network.tcp.parser.strateies;

import io.netty.buffer.Unpooled;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

class PipePayloadParserTest {


    @Test
    void testSplicingUnpack() {
        PipePayloadParser parser = new PipePayloadParser();

        parser.fixed(4)
                .handler((buffer,p) -> {
                    int len = buffer.getInt(0);
                    p.fixed(len);
                })
                .handler((buffer,p)  -> p.result(buffer).complete());


        parser.handlePayload()
                .doOnSubscribe(sb -> {
                    Mono.delay(Duration.ofMillis(100))
                            .subscribe(r -> {

                                {
                                    Buffer buffer = Buffer.buffer(Unpooled.buffer().writeInt(5));
                                    buffer.appendString("1234");
                                    parser.handle(buffer);
                                    parser.handle(Buffer.buffer("5"));
                                }
                                {
                                    Buffer buffer = Buffer.buffer(Unpooled.buffer().writeInt(6));
                                    buffer.appendString("1234");
                                    parser.handle(buffer);
                                    parser.handle(Buffer.buffer("56"));
                                }
                            });
                })
                .take(2)
                .map(bf -> bf.toString(StandardCharsets.UTF_8))
                .as(StepVerifier::create)
                .expectNext("12345", "123456")
                .verifyComplete();
    }


    @Test
    void test() {
        PipePayloadParser parser = new PipePayloadParser();

        parser.fixed(4)
                .handler((buffer,p) -> {
                    int len = buffer.getInt(0);
                    p.fixed(len);
                })
                .handler((buffer,p) -> {
                    p.result(buffer).complete();
                });

        byte[] payload = "hello".getBytes();

        Buffer buffer = Buffer.buffer(payload.length + 4);

        buffer.appendBuffer(Buffer.buffer(Unpooled.buffer().writeInt(payload.length)));
        buffer.appendBytes(payload);

        parser.handlePayload()
                .doOnSubscribe(sb -> {
                    Flux.range(0, 100)
                            .delayElements(Duration.ofMillis(10))
                            .subscribe(i -> {
                                parser.handle(buffer);
                            });
                })
                .take(2)
                .map(bf -> bf.toString(StandardCharsets.UTF_8))
                .as(StepVerifier::create)
                .expectNext("hello", "hello")
                .verifyComplete();


    }


}