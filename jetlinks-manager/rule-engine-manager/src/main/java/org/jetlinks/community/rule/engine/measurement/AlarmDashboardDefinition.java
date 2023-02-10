package org.jetlinks.community.rule.engine.measurement;

import lombok.AllArgsConstructor;
import lombok.Generated;
import lombok.Getter;
import org.jetlinks.community.dashboard.DashboardDefinition;

/**
 * @author bestfeng
 */
@Getter
@AllArgsConstructor
@Generated
public enum AlarmDashboardDefinition implements DashboardDefinition {

    alarm("alarm","告警信息");

    private String id;

    private String name;
}
