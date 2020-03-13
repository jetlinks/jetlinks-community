package org.jetlinks.community.device.web.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
public enum TransportSupportType implements EnumDict<String> {
    ENCODE("编码"), DECODE("解码");
    private String text;

    @Override
    public String getValue() {
        return name();
    }

}
