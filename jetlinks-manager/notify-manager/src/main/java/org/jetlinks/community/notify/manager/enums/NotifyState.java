package org.jetlinks.community.notify.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@Getter
@AllArgsConstructor
public enum NotifyState implements I18nEnumDict<String> {

    success("成功"),
    retrying("重试中"),
    error("失败"),
    cancel("已取消");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
