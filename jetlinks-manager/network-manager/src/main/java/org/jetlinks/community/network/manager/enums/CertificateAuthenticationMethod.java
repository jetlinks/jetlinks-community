package org.jetlinks.community.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.I18nEnumDict;

@Getter
@AllArgsConstructor
public enum CertificateAuthenticationMethod implements I18nEnumDict<String> {
    single("单向认证"),
    binomial("双向认证");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
