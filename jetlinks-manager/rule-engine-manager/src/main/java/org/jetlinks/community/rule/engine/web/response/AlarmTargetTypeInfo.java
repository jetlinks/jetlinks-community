package org.jetlinks.community.rule.engine.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.alarm.AlarmTarget;

/**
 * @author bestfeng
 */
@Getter
@Setter
public class AlarmTargetTypeInfo {

    private String id;

    private String name;


    public static AlarmTargetTypeInfo of(AlarmTarget type) {

        AlarmTargetTypeInfo info = new AlarmTargetTypeInfo();

        info.setId(type.getType());
        info.setName(type.getName());

        return info;

    }
}
