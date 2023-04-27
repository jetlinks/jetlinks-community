package org.jetlinks.community.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@Getter
@AllArgsConstructor
public enum CertificateMode implements I18nEnumDict<String> {
    client("客户端"),
    server("服务端");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}