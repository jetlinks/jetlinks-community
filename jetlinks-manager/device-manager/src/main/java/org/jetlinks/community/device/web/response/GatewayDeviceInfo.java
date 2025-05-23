/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
