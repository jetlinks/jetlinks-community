package org.jetlinks.community.device.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
@Dict("device-type")
@JsonDeserialize(contentUsing = EnumDict.EnumDictJSONDeserializer.class)
public enum DeviceType implements EnumDict<String> {
    device("直连设备"),
    childrenDevice("网关子设备"),
    gateway("网关设备")
    ;

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

//    @Override
//    public boolean isWriteJSONObjectEnabled() {
//        return false;
//    }
}
