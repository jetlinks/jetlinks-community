package org.jetlinks.community.device.web.protocol;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
@Generated
public enum TransportSupportType implements EnumDict<String> {
    ENCODE("编码"), DECODE("解码");
    private String text;

    @Override
    @Generated
    public String getValue() {
        return name();
    }

}
