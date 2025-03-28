package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
@Dict("device-state")
@Generated
public enum DeviceState implements I18nEnumDict<String> {
    notActive("禁用"),
    offline("离线"),
    online("在线");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    @Generated
    public static DeviceState of(byte state) {
        switch (state) {
            case org.jetlinks.core.device.DeviceState.offline:
                return offline;
            case org.jetlinks.core.device.DeviceState.online:
                return online;
            default:
                return notActive;
        }
    }
}
