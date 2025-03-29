package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
public enum AlarmHandleState implements I18nEnumDict<String> {

    processed("已处理"),
    unprocessed("未处理");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
