/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.device.debug.debug;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.TopicUtils;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@Component
@AllArgsConstructor
@Slf4j
public class DeviceDebugSubscriptionProvider implements SubscriptionProvider {
    private final DeviceTraceHelper traceHelper;

    @Override
    public String id() {
        return "device-debug";
    }

    @Override
    public String name() {
        return "设备诊断";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{"/debug/device/*/trace"};
    }

    @Override
    public Flux<?> subscribe(SubscribeRequest request) {
        String deviceId = TopicUtils
            .getPathVariables("/debug/device/{deviceId}/trace", request.getTopic())
            .get("deviceId");
        return traceHelper.startTracing(deviceId);
    }

}
