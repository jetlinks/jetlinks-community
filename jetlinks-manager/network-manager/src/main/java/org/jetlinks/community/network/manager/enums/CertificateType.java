package org.jetlinks.community.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@Getter
@AllArgsConstructor
public enum CertificateType implements I18nEnumDict<String> {
    common("国际标准");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}