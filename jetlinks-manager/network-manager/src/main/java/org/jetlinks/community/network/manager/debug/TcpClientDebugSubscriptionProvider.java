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
package org.jetlinks.community.network.manager.debug;

import io.netty.buffer.Unpooled;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.tcp.TcpMessage;
import org.jetlinks.community.network.tcp.client.TcpClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Component
public class TcpClientDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public TcpClientDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-tcp-client-debug";
    }

    @Override
    public String name() {
        return "TCP客户端调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/tcp/client/*/_send",
            "/network/tcp/client/*/_subscribe"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[4];
        if (request.getTopic().endsWith("_send")) {
            return send(id, request);
        } else {
            return subscribe(id, request);
        }
    }

    public Flux<String> send(String id, SubscribeRequest request) {
        String message = request.getString("request")
            .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));

        byte[] payload=DebugUtils.stringToBytes(message);

        return networkManager
            .<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, id)
            .flatMap(client -> client.send(new TcpMessage(Unpooled.wrappedBuffer(payload))))
            .thenReturn("推送成功")
            .flux();
    }

    @SuppressWarnings("all")
    public Flux<String> subscribe(String id, SubscribeRequest request) {
        String message = request.getString("response").filter(StringUtils::hasText).orElse(null);

        byte[] payload =DebugUtils.stringToBytes(message);

        return networkManager
            .<TcpClient>getNetwork(DefaultNetworkType.TCP_CLIENT, id)
            .flatMapMany(client -> client
                .subscribe()
                .flatMap(msg -> client
                    .send(new TcpMessage(Unpooled.wrappedBuffer(payload)))
                    .thenReturn(msg))
                .map(TcpMessage::toString)
            );
    }


}
