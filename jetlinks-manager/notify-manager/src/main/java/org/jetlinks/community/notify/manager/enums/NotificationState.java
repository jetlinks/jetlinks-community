package org.jetlinks.community.notify.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum NotificationState implements EnumDict<String> {

    unread("未读"),
    read("已读");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
