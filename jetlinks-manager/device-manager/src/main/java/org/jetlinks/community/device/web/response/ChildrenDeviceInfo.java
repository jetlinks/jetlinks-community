package org.jetlinks.community.device.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.enums.DeviceState;

@Getter
@Setter
public class ChildrenDeviceInfo {

    @Schema(description = "子设备ID")
    private String id;

    @Schema(description = "设备名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "子设备状态")
    private DeviceState state;

    public static ChildrenDeviceInfo of(DeviceInstanceEntity instance) {
        ChildrenDeviceInfo deviceInfo = new ChildrenDeviceInfo();
        deviceInfo.setId(instance.getId());
        deviceInfo.setName(instance.getName());
        deviceInfo.setState(instance.getState());
        deviceInfo.setDescription(instance.getDescribe());

        return deviceInfo;
    }
}
