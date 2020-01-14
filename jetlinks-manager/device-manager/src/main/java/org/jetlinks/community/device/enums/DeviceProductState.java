package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
@Dict("device-product-state")
public enum DeviceProductState implements EnumDict<Byte> {
    unregistered("未发布", (byte) 0),
    registered("已发布", (byte) 1),
    other("其它", (byte) -100),
    forbidden("禁用", (byte) -1);

    private String text;

    private Byte value;

    public String getName() {
        return name();
    }

    public static DeviceProductState of(byte state) {
        return EnumDict.findByValue(DeviceProductState.class, state)
                .orElse(other);
    }
}
