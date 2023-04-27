package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum AlarmState implements I18nEnumDict<String> {

    disabled("禁用"),
    enabled("正常");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
