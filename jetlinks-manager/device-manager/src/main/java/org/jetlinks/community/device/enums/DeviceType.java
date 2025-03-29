package org.jetlinks.community.device.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
@Dict("device-type")
//@JsonDeserialize(contentUsing = EnumDict.EnumDictJSONDeserializer.class)
@Generated
public enum DeviceType implements I18nEnumDict<String> {
    device("直连设备"),
    childrenDevice("网关子设备"),
    gateway("网关设备")
    ;

    private final String text;

    @Override
    public String getValue() {
        return name();
    }


}
