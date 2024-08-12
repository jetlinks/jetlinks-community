package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

/**
 * @author bestfeng
 */
@AllArgsConstructor
@Getter
public enum AlarmHandleType implements EnumDict<String> {

    system("系统"),
    user("人工");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
