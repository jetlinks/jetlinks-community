package org.jetlinks.community.network.mqtt.server.vertx;

import lombok.*;
import org.jetlinks.community.network.AbstractServerNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VertxMqttServerProperties extends AbstractServerNetworkConfig {

    //服务实例数量(线程数)
    private int instance = Runtime.getRuntime().availableProcessors();

    //最大消息长度
    private int maxMessageSize = 8096;

    @Override
    public NetworkTransport getTransport() {
        return NetworkTransport.TCP;
    }

    @Override
    public String getSchema() {
        return "mqtt";
    }
}
