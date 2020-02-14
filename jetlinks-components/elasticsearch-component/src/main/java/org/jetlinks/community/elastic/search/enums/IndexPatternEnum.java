package org.jetlinks.community.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor
@Getter
public enum IndexPatternEnum {

    MONTH("month"),
    DAY("day");

    private String value;
}
