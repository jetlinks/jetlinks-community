package org.jetlinks.community.rule.engine.alarm;


import org.hswebframework.web.i18n.LocaleUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * @author bestfeng
 */
@Component
public class SceneAlarmTarget extends AbstractAlarmTarget {

    public static final String TYPE = "scene";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getName() {
        return LocaleUtils
            .resolveMessage("message.rule_engine_alarm_scene", "场景");
    }

    @Override
    public Integer getOrder() {
        return 400;
    }

    @Override
    public Flux<AlarmTargetInfo> doConvert(AlarmData data) {
        return Flux.just(AlarmTargetInfo
            .of(data.getRuleId(),
                data.getRuleName(),
                getType(),
                data.getCreatorId())
            .withSource(TYPE, data.getRuleId(), data.getRuleName()));
    }

}
