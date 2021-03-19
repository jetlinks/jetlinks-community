package org.jetlinks.community.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
@Dict("network-type")
public enum DefaultNetworkType implements NetworkType, EnumDict<String> {

    TCP_CLIENT("TCP客户端"),
    TCP_SERVER("TCP服务"),

    MQTT_CLIENT("MQTT客户端"),
    MQTT_SERVER("MQTT服务"),

    HTTP_CLIENT("HTTP客户端"),
    HTTP_SERVER("HTTP服务"),

    WEB_SOCKET_CLIENT("WebSocket客户端"),
    WEB_SOCKET_SERVER("WebSocket服务"),

    UDP("UDP"),

    COAP_CLIENT("CoAP客户端"),
    COAP_SERVER("CoAP服务"),

    ;

    static {
        NetworkTypes.register(Arrays.asList(DefaultNetworkType.values()));
    }

    private final String name;

    @Override
    public String getId() {
        return name();
    }


    @Override
    public String getText() {
        return name;
    }

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public Object getWriteJSONObject() {
        if (isWriteJSONObjectEnabled()) {
            Map<String, Object> jsonObject = new HashMap<>();
            jsonObject.put("value", getValue());
            jsonObject.put("text", getText());
            jsonObject.put("name", getText());
            return jsonObject;
        }
        return name();
    }
}
