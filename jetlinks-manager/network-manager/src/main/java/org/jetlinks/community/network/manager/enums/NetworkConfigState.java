package org.jetlinks.community.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
@Dict("network-config-state")
public enum NetworkConfigState implements EnumDict<String> {
    enabled("已启动", "enabled"),
    paused("已暂停", "paused"),
    disabled("已停止", "disabled");

    private String text;

    private String value;

}
