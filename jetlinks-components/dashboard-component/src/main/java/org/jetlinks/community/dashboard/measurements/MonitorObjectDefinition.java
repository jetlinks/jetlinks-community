package org.jetlinks.community.dashboard.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.dashboard.ObjectDefinition;

@Getter
@AllArgsConstructor
public enum MonitorObjectDefinition implements ObjectDefinition {

    cpu("CPU"),
    memory("内存");

    private String name;

    @Override
    public String getId() {
        return name();
    }
}
