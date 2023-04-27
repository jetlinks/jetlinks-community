package org.jetlinks.community.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
@Generated
@Dict("device-gateway-state")
public enum DeviceGatewayState implements I18nEnumDict<String> {
    enabled("正常", "enabled"),
    paused("已暂停", "paused"),
    disabled("禁用", "disabled");

    private final String text;

    private final String value;

}
