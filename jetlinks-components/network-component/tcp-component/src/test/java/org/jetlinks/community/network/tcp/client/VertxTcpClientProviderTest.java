package org.jetlinks.community.network.tcp.client;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.parser.DefaultPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

class VertxTcpClientProviderTest {


    @Test
    void test() {
        Vertx vertx = Vertx.vertx();

        vertx.createNetServer()
                .connectHandler(socket -> {
                    socket.write("tes");
                    socket.write("ttest");
                })
                .listen(12311);

        VertxTcpClientProvider provider = new VertxTcpClientProvider(id -> Mono.empty(), vertx, new DefaultPayloadParserBuilder());

        TcpClientProperties properties = new TcpClientProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(12311);
        properties.setParserType(PayloadParserType.FIXED_LENGTH);
        properties.setParserConfiguration(Collections.singletonMap("size", 4));
        properties.setOptions(new NetClientOptions());


        provider.createNetwork(properties)
                .subscribe()
                .map(TcpMessage::getPayload)
                .map(buf -> buf.toString(StandardCharsets.UTF_8))
                .take(2)
                .as(StepVerifier::create)
                .expectNext("test", "test")
                .verifyComplete();

    }

}