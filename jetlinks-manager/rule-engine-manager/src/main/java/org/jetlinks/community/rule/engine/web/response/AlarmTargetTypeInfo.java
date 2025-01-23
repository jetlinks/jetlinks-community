package org.jetlinks.community.rule.engine.web.response;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.alarm.AlarmTarget;

import java.util.List;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmTargetTypeInfo {

    private String id;

    private String name;

    private List<String> supportTriggers;

    public static AlarmTargetTypeInfo of(AlarmTarget type) {

        AlarmTargetTypeInfo info = new AlarmTargetTypeInfo();

        info.setId(type.getType());
        info.setName(type.getName());

        return info;

    }

    public AlarmTargetTypeInfo with(List<String> supportTriggers) {
        this.supportTriggers = supportTriggers;
        return this;
    }

    public String getName() {
        return LocaleUtils.resolveMessage("message.rule_engine_alarm_" + id, name);
    }
}
