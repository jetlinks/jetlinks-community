package org.jetlinks.community.network.tcp.client;

import io.vertx.core.net.NetClientOptions;
import lombok.*;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpClientProperties {

    private String id;

    private int port;

    private String host;

    private String certId;

    private boolean ssl;

    private PayloadParserType parserType;

    private Map<String,Object> parserConfiguration;

    private NetClientOptions options;

    private boolean enabled;

}
