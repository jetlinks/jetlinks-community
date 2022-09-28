package org.jetlinks.community.rule.engine.alarm;

import org.jetlinks.community.rule.engine.scene.SceneData;
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
    public Flux<AlarmTargetInfo> convert(SceneData data) {
        return Flux.just(AlarmTargetInfo
                             .of(data.getRule().getId(),
                                 data.getRule().getName(),
                                 getType()));
    }

}
