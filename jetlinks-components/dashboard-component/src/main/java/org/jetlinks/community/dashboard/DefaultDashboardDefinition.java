package org.jetlinks.community.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@AllArgsConstructor
@Getter
public enum DefaultDashboardDefinition implements DashboardDefinition, EnumDict<String> {

    systemMonitor("系统监控"),
    jvmMonitor("jvm监控")

    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }

    @Override
    public String getValue() {
        return getId();
    }

    @Override
    public String getText() {
        return name;
    }
}
