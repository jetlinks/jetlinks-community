package org.jetlinks.community.rule.engine.alarm;

import org.jetlinks.community.rule.engine.scene.SceneData;
import reactor.core.publisher.Flux;

/**
 * 告警目标
 *
 * @author bestfeng
 */
public interface AlarmTarget {


    String getType();

    String getName();

    Flux<AlarmTargetInfo> convert(SceneData data);

    static AlarmTarget of(String type) {
        return AlarmTargetSupplier
            .get()
            .getByType(type)
            .orElseThrow(() -> new UnsupportedOperationException("error.unsupported_alarm_target"));
    }
}
