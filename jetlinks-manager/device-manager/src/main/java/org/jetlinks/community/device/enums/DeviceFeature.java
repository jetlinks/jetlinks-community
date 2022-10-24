package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.core.metadata.Feature;

@Dict("device-feature")
@Getter
@AllArgsConstructor
public enum DeviceFeature implements EnumDict<String> , Feature {
    selfManageState("子设备自己管理状态")


    ;
    private final String text;

    @Override
    @Generated
    public String getValue() {
        return name();
    }

    @Override
    public String getId() {
        return getValue();
    }

    @Override
    public String getName() {
        return text;
    }
}
