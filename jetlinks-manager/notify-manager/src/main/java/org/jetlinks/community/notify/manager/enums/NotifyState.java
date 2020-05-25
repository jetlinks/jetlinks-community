package org.jetlinks.community.notify.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum NotifyState implements EnumDict<String> {

    success("成功"),
    error("失败");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
