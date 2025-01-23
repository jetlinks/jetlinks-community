package org.jetlinks.community.rule.engine.alarm;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * @author bestfeng
 */
@Component
public class SceneAlarmTarget implements AlarmTarget {

    public static final String TYPE = "scene";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return "场景";
    }

    @Override
    public Flux<AlarmTargetInfo> convert(AlarmData data) {
        return Flux.just(AlarmTargetInfo
                             .of(data.getRuleId(),
                                 data.getRuleName(),
                                 getType())
                             .withSource(TYPE, data.getRuleId(), data.getRuleName()));
    }

}
