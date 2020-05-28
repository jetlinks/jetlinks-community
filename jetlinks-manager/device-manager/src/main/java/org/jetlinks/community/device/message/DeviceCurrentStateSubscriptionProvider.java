package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class DeviceCurrentStateSubscriptionProvider implements SubscriptionProvider {

    private final LocalDeviceInstanceService instanceService;

    @Override
    public String id() {
        return "device-state-subscriber";
    }

    @Override
    public String name() {
        return "设备当前状态消息";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/device-current-state"
        };
    }

    @Override
    @SuppressWarnings("all")
    public Flux<Map<String, Object>> subscribe(SubscribeRequest request) {
        List<String> deviceId = request.get("deviceId")
            .map(List.class::cast)
            .orElseThrow(() -> new IllegalArgumentException("deviceId不能为空"));

        return Flux
            .fromIterable(deviceId)
            .buffer(200)
            .concatMap(buf -> {
                return instanceService.createQuery()
                    .select(DeviceInstanceEntity::getId, DeviceInstanceEntity::getState)
                    .in(DeviceInstanceEntity::getId, buf)
                    .fetch();
            })
            .map(instance -> Collections.singletonMap(instance.getId(), instance.getState().name()));

    }
}
