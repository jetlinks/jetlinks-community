package org.jetlinks.community.elastic.search.aggreation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@Getter
public enum OrderType {

    COUNT("计数"),
    KEY("分组值");

    private String text;
}
