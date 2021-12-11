package org.jetlinks.community.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

/**
 * 菜单作用域
 * @author zeje
 */
@Getter
@AllArgsConstructor
public enum MenuScope implements EnumDict<String> {

    admin("管理后台");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

}
