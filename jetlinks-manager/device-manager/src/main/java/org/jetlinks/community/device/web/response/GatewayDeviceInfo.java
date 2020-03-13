package org.jetlinks.community.device.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.enums.DeviceState;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 网关设备信息
 */
@Getter
@Setter
public class GatewayDeviceInfo {

    private String id;

    private String name;

    private String description;

    private DeviceState state;

    private List<ChildrenDeviceInfo> children;


    public static GatewayDeviceInfo of(DeviceInstanceEntity gateway, List<DeviceInstanceEntity> children) {

        GatewayDeviceInfo info = new GatewayDeviceInfo();
        info.setId(gateway.getId());
        info.setName(gateway.getName());
        info.setDescription(gateway.getDescribe());
        info.setState(gateway.getState());
        info.setChildren(children.stream().map(ChildrenDeviceInfo::of).collect(Collectors.toList()));

        return info;
    }
}
