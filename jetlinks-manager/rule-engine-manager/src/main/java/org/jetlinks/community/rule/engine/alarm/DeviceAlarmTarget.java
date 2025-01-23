package org.jetlinks.community.rule.engine.alarm;

import org.jetlinks.community.rule.engine.scene.internal.triggers.DeviceTriggerProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author bestfeng
 */
@Component
public class DeviceAlarmTarget extends AbstractAlarmTarget {

    @Override
    public String getType() {
        return "device";
    }

    @Override
    public String getName() {
        return "设备";
    }

    @Override
    public Flux<AlarmTargetInfo> doConvert(AlarmData data) {
        Map<String, Object> output = data.getOutput();
        String deviceId = AbstractAlarmTarget.getFromOutput("deviceId", output).map(String::valueOf).orElse(null);
        String deviceName = AbstractAlarmTarget.getFromOutput("deviceName", output).map(String::valueOf).orElse(deviceId);

        if (deviceId == null) {
            return Flux.empty();
        }

        return Flux.just(AlarmTargetInfo.of(deviceId, deviceName, getType()));
    }

    @Override
    public boolean isSupported(String trigger) {
        return DeviceTriggerProvider.PROVIDER.equals(trigger);
    };

}
