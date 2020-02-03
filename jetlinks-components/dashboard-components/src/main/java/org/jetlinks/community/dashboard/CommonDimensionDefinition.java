package org.jetlinks.community.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用维度定义
 *
 * @author zhouhao
 */
@AllArgsConstructor
@Getter
public enum CommonDimensionDefinition implements DimensionDefinition {
    realTime("实时"),
    history("历史");

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
