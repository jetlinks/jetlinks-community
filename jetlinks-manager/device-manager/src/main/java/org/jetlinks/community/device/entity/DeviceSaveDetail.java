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
package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DeviceSaveDetail {

    //设备ID
    @Schema(defaultValue = "设备ID")
    private String id;

    //设备名称
    @Schema(defaultValue = "设备名称")
    private String name;

    //型号ID
    @Schema(defaultValue = "产品ID")
    private String productId;

    @Hidden
    private String creatorId;

    @Hidden
    private String creatorName;

    //设备配置信息
    @Schema(defaultValue = "配置信息")
    private Map<String, Object> configuration;

    //标签
    @Schema(defaultValue = "标签信息")
    private List<DeviceTagEntity> tags;

    //父设备ID
    @Schema(defaultValue = "父设备ID")
    private String parentId;

    public List<DeviceTagEntity> getTags() {
        if (CollectionUtils.isEmpty(tags)) {
            return Collections.emptyList();
        }
        return tags;
    }

    public DeviceInstanceEntity toInstance() {
        DeviceInstanceEntity entity = DeviceInstanceEntity.of();

        entity.setId(id);
        entity.setName(name);
        entity.setProductId(productId);
        entity.setConfiguration(configuration);
        entity.setCreatorId(creatorId);
        entity.setCreatorName(creatorName);
        entity.setCreateTimeNow();
        entity.setParentId(parentId);
        return entity;
    }

    public void prepare() {
        for (DeviceTagEntity tag : getTags()) {
            Assert.hasText(tag.getKey(), "tag.key不能为空");
            Assert.hasText(tag.getValue(), "tag.value不能为空");
            Assert.hasText(tag.getType(), "tag.type不能为空");

            tag.setId(DeviceTagEntity.createTagId(id, tag.getKey()));
            tag.setDeviceId(id);
        }
    }

}
