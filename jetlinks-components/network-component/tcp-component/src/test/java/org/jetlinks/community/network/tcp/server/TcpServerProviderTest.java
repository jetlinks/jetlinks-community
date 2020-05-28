package org.jetlinks.community.network.tcp.server;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.jetlinks.community.network.tcp.parser.DefaultPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Slf4j
class TcpServerProviderTest {


    static TcpServer tcpServer;

    @BeforeAll
    static void init() {
        TcpServerProperties properties = TcpServerProperties.builder()
            .id("test")
            .port(8080)
            .options(new NetServerOptions())
            .parserType(PayloadParserType.FIXED_LENGTH)
            .parserConfiguration(Collections.singletonMap("size", 5))
            .build();

        TcpServerProvider provider = new TcpServerProvider((id) -> Mono.empty(), Vertx.vertx(), new DefaultPayloadParserBuilder());

        tcpServer = provider.createNetwork(properties);
    }


    @Test
    void test() {

        Vertx.vertx().createNetClient()
            .connect(8080, "localhost", handle -> {
                if (handle.succeeded()) {
                    //模拟粘包，同时发送2个包
                    handle.result().write("hellohello", r -> {
                        if (r.succeeded()) {
                            log.info("tcp客户端消息发送成功");
                        } else {
                            log.error("tcp客户端消息发送错误", r.cause());
                        }
                    });
                } else {
                    log.error("创建tcp客户端错误", handle.cause());
                }
            });


        tcpServer.handleConnection()
            .flatMap(TcpClient::subscribe)
            .map(TcpMessage::getPayload)
            .map(payload -> payload.toString(StandardCharsets.UTF_8))
            .take(2)
            .as(StepVerifier::create)
            .expectNext("hello", "hello")//收到2个完整的包
            .verifyComplete();
    }


}