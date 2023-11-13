package org.jetlinks.community.rule.engine.alarm;

import reactor.core.publisher.Flux;

/**
 * @author bestfeng
 */

public class OtherAlarmTarget implements AlarmTarget {

    @Override
    public String getType() {
        return "other";
    }

    @Override
    public String getName() {
        return "其它";
    }

    @Override
    public Flux<AlarmTargetInfo> convert(AlarmData data) {
        return Flux.just(AlarmTargetInfo
                             .of(data.getRuleId(),
                                 data.getRuleName(),
                                 getType()));
    }

}
