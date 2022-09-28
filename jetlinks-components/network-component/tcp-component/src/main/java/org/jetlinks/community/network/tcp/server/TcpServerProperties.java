package org.jetlinks.community.network.tcp.server;

import io.vertx.core.net.SocketAddress;
import lombok.*;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.network.AbstractServerNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.rule.engine.executor.PayloadType;

import java.util.HashMap;
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
public class TcpServerProperties extends AbstractServerNetworkConfig implements ValueObject {

    private PayloadType payloadType;

    private PayloadParserType parserType;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private boolean tcpKeepAlive = false;

    //服务实例数量(线程数)
    private int instance = Runtime.getRuntime().availableProcessors();

    public SocketAddress createSocketAddress() {
        return SocketAddress.inetSocketAddress(port, host);
    }

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }

    @Override
    public NetworkTransport getTransport() {
        return NetworkTransport.TCP;
    }

    @Override
    public String getSchema() {
        return "tcp";
    }
}
