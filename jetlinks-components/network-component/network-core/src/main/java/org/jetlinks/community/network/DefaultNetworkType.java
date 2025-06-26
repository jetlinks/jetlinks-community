/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.i18n.LocaleUtils;

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
    public String getName() {
        return getI18nMessage(LocaleUtils.current());
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
