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
    realTime("实时数据"),
    history("历史数据"),
    current("当前数据"),
    agg("聚合数据");

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
