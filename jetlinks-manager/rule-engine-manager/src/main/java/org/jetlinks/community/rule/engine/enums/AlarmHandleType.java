package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum AlarmHandleType implements I18nEnumDict<String> {

    system("系统"),
    user("人工");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
