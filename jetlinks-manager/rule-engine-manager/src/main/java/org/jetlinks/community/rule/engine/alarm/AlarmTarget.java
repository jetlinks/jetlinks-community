package org.jetlinks.community.rule.engine.alarm;

import reactor.core.publisher.Flux;

/**
 * 告警目标
 *
 * @author bestfeng
 */
public interface AlarmTarget {


    String getType();

    String getName();

    default Integer getOrder(){
        return Integer.MAX_VALUE;
    };

    Flux<AlarmTargetInfo> convert(AlarmData data);

    default boolean isSupported(String trigger) {
        return true;
    };

    static AlarmTarget of(String type) {
        return AlarmTargetSupplier
            .get()
            .getByType(type)
            .orElseThrow(() -> new UnsupportedOperationException("error.unsupported_alarm_target"));
    }
}
