package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.I18nEnumDict;

@Getter
@AllArgsConstructor
@Dict( "rule-instance-state")
public enum RuleInstanceState implements I18nEnumDict<String> {
    started("正常"),
    disable("禁用");
    private final String text;

    @Override
    public String getValue() {
        return name();
    }

}
