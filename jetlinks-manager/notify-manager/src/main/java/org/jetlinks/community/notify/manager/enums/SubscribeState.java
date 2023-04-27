package org.jetlinks.community.notify.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum SubscribeState implements EnumDict<String> {
    enabled("正常"),
    disabled("禁用");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
