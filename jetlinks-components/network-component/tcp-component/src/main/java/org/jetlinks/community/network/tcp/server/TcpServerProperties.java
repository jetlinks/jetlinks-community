package org.jetlinks.community.network.tcp.server;

import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import lombok.*;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpServerProperties {

    private String id;

    private NetServerOptions options;

    private PayloadType payloadType;

    private PayloadParserType parserType;

    private Map<String, Object> parserConfiguration;

    private String host;

    private int port;

    private boolean ssl;

    private String certId;


    public SocketAddress createSocketAddress() {
        if (StringUtils.isEmpty(host)) {
            host = "localhost";
        }
        return SocketAddress.inetSocketAddress(port, host);
    }
}
