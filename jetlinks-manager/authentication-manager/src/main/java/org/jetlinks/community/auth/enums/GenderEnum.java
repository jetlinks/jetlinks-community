package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

/**
 * @author: tangchao
 * @since: 2.2
 */
@Getter
@AllArgsConstructor
public enum GenderEnum implements EnumDict<String> {
    man("男"),
    unknown("未知"),
    female("女");

    private final String text;


    @Override
    public String getValue() {
        return name();
    }
}
