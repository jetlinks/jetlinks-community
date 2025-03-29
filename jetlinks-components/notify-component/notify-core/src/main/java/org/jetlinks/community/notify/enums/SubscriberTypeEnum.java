package org.jetlinks.community.notify.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.notify.subscription.SubscribeType;

/**
 * @author bestfeng
 */
@AllArgsConstructor
@Getter
public enum SubscriberTypeEnum implements SubscribeType, I18nEnumDict<String> {


    alarm("告警"),
    systemEvent("系统事件"),
    businessEvent("业务事件"),
    other("其它");

    private final String text;


    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getId() {
        return getValue();
    }

    @Override
    public String getName() {
        return getI18nMessage(LocaleUtils.current());
    }
}
