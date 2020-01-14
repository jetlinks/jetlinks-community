package org.jetlinks.community.network.mqtt.client;

import io.vertx.mqtt.MqttClientOptions;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MqttClientProperties {
    private String id;
    private String clientId;
    private String host;
    private int port;

    private String username;
    private String password;

    private String certId;
    private MqttClientOptions options;
    private boolean ssl;

}
