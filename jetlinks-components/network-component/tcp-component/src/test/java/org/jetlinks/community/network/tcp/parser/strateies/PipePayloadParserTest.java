package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import org.jetlinks.community.network.utils.BytesUtils;
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
                .handler(buffer -> {
                    int len = BytesUtils.lowBytesToInt(buffer.getBytes());
                    parser.fixed(len);
                })
                .handler(buffer -> parser.result(buffer).complete());


        parser.handlePayload()
                .doOnSubscribe(sb -> {
                    Mono.delay(Duration.ofMillis(100))
                            .subscribe(r -> {
                                Buffer buffer = Buffer.buffer(BytesUtils.toLowBytes(5));
                                buffer.appendString("1234");
                                parser.handle(buffer);
                                parser.handle(Buffer.buffer("5"));

                                parser.handle(Buffer.buffer(new byte[]{0, 0}));
                                parser.handle(Buffer.buffer(new byte[]{0, 6}).appendString("12"));
                                parser.handle(Buffer.buffer("3456"));
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
                .handler(buffer -> {
                    int len = BytesUtils.highBytesToInt(buffer.getBytes());
                    parser.fixed(len);
                })
                .handler(buffer -> {
                    parser.result(buffer)
                            .complete();
                });

        byte[] payload = "hello".getBytes();

        Buffer buffer = Buffer.buffer(payload.length + 4);

        buffer.appendBytes(BytesUtils.toHighBytes(payload.length));
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