package org.jetlinks.community.rule.engine.scene;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
@Deprecated
public enum TriggerType implements EnumDict<String> {
    manual("手动触发"),
    timer("定时触发"),
    device("设备触发");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public boolean isWriteJSONObjectEnabled() {
        return false;
    }
}
