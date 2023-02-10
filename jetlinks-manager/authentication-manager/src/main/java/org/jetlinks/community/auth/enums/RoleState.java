package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

/**
 * 角色状态
 * @author zhouhao
 * @since 2.0
 */
@Getter
@AllArgsConstructor
public enum RoleState implements EnumDict<String> {

    enabled("正常"),
    disabled("已禁用");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

}
