package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

/**
 * @author hsy  2024/12/11
 * @since 2.3
 */
@Getter
@AllArgsConstructor
public enum RegisterEnum implements EnumDict<String> {
    myself("自主注册"),
    backstage("后台注册");

    private final String text;


    @Override
    public String getValue() {
        return name();
    }
}
