package org.jetlinks.community.rule.engine.alarm;

import org.jetlinks.community.rule.engine.scene.SceneData;
import org.jetlinks.reactor.ql.utils.CastUtils;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * @author bestfeng
 */

public class DeviceAlarmTarget implements AlarmTarget {

    @Override
    public String getType() {
        return "device";
    }

    @Override
    public String getName() {
        return "设备";
    }

    @Override
    public Flux<AlarmTargetInfo> convert(SceneData data) {
        Map<String, Object> output = data.getOutput();
        String deviceId = CastUtils.castString(output.get("deviceId"));
        String deviceName = CastUtils.castString(output.getOrDefault("deviceName", deviceId));
        return Flux.just(AlarmTargetInfo.of(deviceId, deviceName, getType()));
    }

}
