package org.jetlinks.community.gateway.monitor.measurements;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.community.dashboard.ObjectDefinition;

@AllArgsConstructor
@Getter
public enum GatewayObjectDefinition implements ObjectDefinition {
    deviceGateway("设备网关"),
    session("会话")
    ;

    private String name;

    @Override
    public String getId() {
        return name();
    }

}
