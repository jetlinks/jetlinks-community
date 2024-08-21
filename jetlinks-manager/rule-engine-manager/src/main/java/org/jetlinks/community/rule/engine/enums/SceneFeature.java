package org.jetlinks.community.rule.engine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.metadata.Feature;

/**
 * 场景特性.
 *
 * @author zhangji 2024/6/11
 */
@AllArgsConstructor
@Getter
public enum SceneFeature implements Feature {

    none("不存在任何特性"),
    alarmTrigger("存在告警触发动作"),
    alarmReliever("存在告警解除动作");

    private final String name;

    @Override
    public String getId() {
        return name();
    }

}
