package org.jetlinks.community.device.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
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

    @Schema(description = "网关设备ID")
    private String id;

    @Schema(description = "网关设备名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "网关设备状态")
    private DeviceState state;

    @Schema(description = "子设备信息")
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
