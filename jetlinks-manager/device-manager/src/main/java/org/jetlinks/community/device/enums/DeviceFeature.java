package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

@Dict("device-feature")
@Getter
@AllArgsConstructor
public enum DeviceFeature implements EnumDict<String> {
    selfManageState("子设备自己管理状态")


    ;
    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
