package org.jetlinks.community.elastic.search.aggreation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public enum AggregationType {
    AVG("平均"),
    MAX("最大"),
    COUNT("计数"),
    MIN("最小"),
    SUM("总数"),
    STATS("统计汇总"),
    EXTENDED_STATS("扩展统计"),
    CARDINALITY("基数"),//去重统计
    VALUE_COUNT("非空值计数"),
    TERMS("字段项"),
    RANGE("范围"),
    DATE_HISTOGRAM("直方图"),
    DATE_RANGE("时间范围");

    private String text;
}
