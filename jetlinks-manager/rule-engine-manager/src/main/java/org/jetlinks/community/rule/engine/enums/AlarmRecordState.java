package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum AlarmRecordState implements I18nEnumDict<String> {

    warning("告警中"),
    normal("无告警");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
