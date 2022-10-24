package org.jetlinks.community.rule.engine.measurement;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.community.dashboard.ObjectDefinition;

@Getter
@AllArgsConstructor
@Generated
public enum AlarmObjectDefinition implements ObjectDefinition {

    record("告警记录");

    @Override
    public String getId() {
        return name();
    }

    private String name;
}
