package org.jetlinks.community.network.mqtt.client;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.network.AbstractClientNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;
import org.jetlinks.community.network.AbstractClientNetworkConfig;
import org.jetlinks.community.network.resource.NetworkTransport;

/**
 * MQTT Client 配置信息
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
public class MqttClientProperties extends AbstractClientNetworkConfig {

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 证书ID
     */
    private String certId;

    //最大消息长度
    private int maxMessageSize = 0XFFFFFF;

    //共享订阅前缀
    private String topicPrefix;

    /**
     * TSL
     */
    private boolean secure;

    @Override
    public NetworkTransport getTransport() {
        return NetworkTransport.TCP;
    }

    @Override
    public String getSchema() {
        return isSecure()?"mqtts":"mqtt";
    }
}
