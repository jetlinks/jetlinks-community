package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;
import org.jetlinks.community.OperationType;

@AllArgsConstructor
@Getter
public enum DeviceOperationType implements OperationType , I18nEnumDict<String> {
    transparentEncode("数据解析-编码"),
    transparentDecode("数据解析-解码"),

    ;

    private final String name;

    @Override
    public String getValue() {
        return getId();
    }

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String getText() {
        return getName();
    }
}
