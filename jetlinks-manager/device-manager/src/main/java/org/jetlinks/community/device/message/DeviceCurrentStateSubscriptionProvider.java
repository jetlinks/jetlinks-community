package org.jetlinks.community.device.message;

import lombok.AllArgsConstructor;
import lombok.Generated;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.gateway.external.SubscriptionProvider;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class DeviceCurrentStateSubscriptionProvider implements SubscriptionProvider {

    private final ReactiveRepository<DeviceInstanceEntity, String> deviceRepository;

    @Override
    @Generated
    public String id() {
        return "device-state-subscriber";
    }

    @Override
    @Generated
    public String name() {
        return "设备当前状态消息";
    }

    @Override
    @Generated
    public String[] getTopicPattern() {
        return new String[]{
            "/device-current-state"
        };
    }

    @Override
    @SuppressWarnings("all")
    @Generated
    public Flux<Map<String, Object>> subscribe(SubscribeRequest request) {
        List<Object> deviceId = request
            .get("deviceId")
            .map(CastUtils::castArray)
            .orElseThrow(() -> new IllegalArgumentException("error.deviceId_cannot_be_empty"));

        return Flux
            .fromIterable(deviceId)
            .buffer(200)
            .concatMap(buf -> {
                return deviceRepository
                    .createQuery()
                    .select(DeviceInstanceEntity::getId, DeviceInstanceEntity::getState)
                    .in(DeviceInstanceEntity::getId, buf)
                    .fetch();
            })
            .map(instance -> Collections.singletonMap(instance.getId(), instance.getState().name()));

    }
}
