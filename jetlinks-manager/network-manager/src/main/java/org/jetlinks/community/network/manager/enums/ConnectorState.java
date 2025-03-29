package org.jetlinks.community.network.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
@Dict
@Generated
public enum ConnectorState implements I18nEnumDict<String> {
    running("运行中", "running"),
    paused("已暂停", "paused"),
    stopped("已停止", "stopped");

    private String text;

    private String value;

}