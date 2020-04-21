package org.jetlinks.pro.visualization.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
public enum DataVisualizationState implements EnumDict<String> {

    enabled("启用"),
    disabled("禁用")

    ;
    private final String text;

    @Override
    public String getValue() {
        return name();
    }
}
