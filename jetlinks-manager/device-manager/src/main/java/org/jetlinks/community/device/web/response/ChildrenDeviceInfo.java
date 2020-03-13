package org.jetlinks.community.device.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.enums.DeviceState;

@Getter
@Setter
public class ChildrenDeviceInfo {

    private String id;

    private String name;

    private String description;

    private DeviceState state;

    public static ChildrenDeviceInfo of(DeviceInstanceEntity instance){
        ChildrenDeviceInfo deviceInfo=new ChildrenDeviceInfo();
        deviceInfo.setId(instance.getId());
        deviceInfo.setName(instance.getName());
        deviceInfo.setState(instance.getState());
        deviceInfo.setDescription(instance.getDescribe());

        return deviceInfo;
    }
}
