package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
@Dict("device-product-state")
@Generated
public enum DeviceProductState implements I18nEnumDict<Byte> {
    unregistered("正常", (byte) 0),
    registered("禁用", (byte) 1),
    other("其它", (byte) -100),
    forbidden("禁用", (byte) -1);

    private final String text;

    private final Byte value;

    public String getName() {
        return name();
    }

    public static DeviceProductState of(byte state) {
        return EnumDict.findByValue(DeviceProductState.class, state)
                .orElse(other);
    }
}
