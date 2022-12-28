package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author bestfeng
 */
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Getter
@Setter
public class AlarmTargetInfo {

    private String targetId;

    private String targetName;

    private String targetType;

    private String sourceType;

    private String sourceId;

    private String sourceName;

    public static AlarmTargetInfo of(String targetId, String targetName, String targetType) {
        return AlarmTargetInfo.of(targetId, targetName, targetType, null, null, null);
    }

    public AlarmTargetInfo withTarget(String type, String id, String name) {
        this.targetType = type;
        this.targetId = id;
        this.targetName = name;
        return this;
    }

    public AlarmTargetInfo withSource(String type, String id, String name) {
        this.sourceType = type;
        this.sourceId = id;
        this.sourceName = name;
        return this;
    }
}
