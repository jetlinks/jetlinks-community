package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 自定义用户类型.
 *
 * @author zhangji 2022/12/9
 */
@Getter
@AllArgsConstructor
public class UndefinedUserEntityType implements UserEntityType {

    private final String id;

    private final String name;
}
