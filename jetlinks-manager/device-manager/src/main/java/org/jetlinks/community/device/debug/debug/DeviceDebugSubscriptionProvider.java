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
