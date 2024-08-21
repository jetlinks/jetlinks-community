package org.jetlinks.community.rule.engine.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class ShakeLimitResult<T> {

    private String groupKey;
    private long times;
    private T element;

}
