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
package org.jetlinks.community.device.web.excel;

import com.alibaba.fastjson.JSONObject;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.metadata.ConfigPropertyMetadata;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.community.device.enums.DeviceType;
import org.jetlinks.community.io.excel.ExcelUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotBlank;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Generated
public class DeviceExcelInfo implements Jsonable {

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "设备ID")
    @NotBlank(message = "设备ID不能为空")
    private String id;

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "设备名称")
    @NotBlank(message = "设备名称不能为空")
    private String name;

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "产品名称", ignoreRead = true)
    private String productName;

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "设备类型", ignoreRead = true)
    private DeviceType deviceType;

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "父设备ID", ignoreRead = true)
    private String parentId;

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "状态", ignoreRead = true)
    private String state;

    private List<DeviceTagEntity> tags = new ArrayList<>();

    private DeviceInstanceEntity device;

    private Map<String, PropertyMetadata> tagMapping;

    private Map<String, ConfigPropertyMetadata> configMapping;

    private Map<String, Object> configuration = new HashMap<>();

    private long rowNumber;

    public void config(String key, Object value) {
        if (value == null || value instanceof String && !StringUtils.hasText((String) value)) {
            return;
        }
        configuration.put(key, value);
    }

    public static DeviceExcelInfo of(){
        return EntityFactoryHolder.newInstance(DeviceExcelInfo.class,DeviceExcelInfo::new);
    }

    public void tag(String key, String name, Object value, String type) {
        if (ObjectUtils.isEmpty(value)) {
            return;
        }
        DeviceTagEntity entity = new DeviceTagEntity();
        entity.setKey(key);
        entity.setValue(String.valueOf(value));
        entity.setName(name);
        entity.setDeviceId(id);
        entity.setType(type);
        entity.setId(DeviceTagEntity.createTagId(id, key));
        tags.add(entity);
    }

    public void setId(String id) {
        this.id = id;
        for (DeviceTagEntity tag : tags) {
            tag.setDeviceId(id);
            tag.setId(DeviceTagEntity.createTagId(tag.getDeviceId(), tag.getKey()));
        }
    }

    public void with(String key, Object value) {
        FastBeanCopier.copy(Collections.singletonMap(key, value), this);
    }

    public DeviceExcelInfo with(DeviceInstanceEntity device){
        FastBeanCopier.copy(device,this,"state");
        this.setState(device.getState().getText());
        return this;
    }

    public void withConfiguration(Map<String,Object> configuration){
        this.configuration.putAll(configuration);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> val = FastBeanCopier.copy(this, new HashMap<>());
        for (DeviceTagEntity tag : tags) {
            val.put(tag.getKey(), tag.getValue());
        }
        return val;
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject(toMap());
    }

    public static List<ExcelHeader> getTemplateHeaderMapping(DeviceExcelFilterColumns filterColumns,
                                                             List<PropertyMetadata> tags,
                                                             List<ConfigPropertyMetadata> configs) {
        List<ExcelHeader> arr =
            ExcelUtils.getHeadersForRead(
                          EntityFactoryHolder.newInstance(DeviceExcelInfo.class,DeviceExcelInfo::new).getClass())
                      .stream()
                      .filter(a -> !filterColumns.getColumns().contains(a.getKey()))
                      .collect(Collectors.toList());

        return addExtHeaders(arr, tags, configs);
    }

    public static List<ExcelHeader> addExtHeaders(List<ExcelHeader> headers,
                                                  List<PropertyMetadata> tags,
                                                  List<ConfigPropertyMetadata> configs) {
        for (PropertyMetadata tag : tags) {
            headers.add(new ExcelHeader(tag.getId(), StringUtils.hasText(tag.getName()) ? tag.getName() : tag.getId(), CellDataType.STRING));
        }

        for (ConfigPropertyMetadata config : configs) {
            headers.add(new ExcelHeader("configuration." + config.getProperty(),
                                        StringUtils.hasText(config.getName())
                                            ? config.getName()
                                            : config.getProperty(), CellDataType.STRING));
        }
        return headers;
    }

    public static List<ExcelHeader> getExportHeaderMapping(DeviceExcelFilterColumns filterColumns,
                                                           List<PropertyMetadata> tags,
                                                           List<ConfigPropertyMetadata> configs) {
        List<ExcelHeader> arr =
            ExcelUtils.getHeadersForWrite(EntityFactoryHolder.newInstance(DeviceExcelInfo.class,DeviceExcelInfo::new).getClass())
                      .stream()
                      .filter(a -> !filterColumns.getColumns().contains(a.getKey()))
                      .collect(Collectors.toList());

        return addExtHeaders(arr, tags, configs);
    }

    @Deprecated
    public static Map<String, String> getImportHeaderMapping() {
        Map<String, String> mapping = new HashMap<>();

        mapping.put("设备ID", "id");
        mapping.put("设备名称", "name");
        mapping.put("名称", "name");

//        mapping.put("设备型号", "productName");
//        mapping.put("产品型号", "productName");

        mapping.put("父设备ID", "parentId");

        return mapping;
    }

    public DeviceExcelInfo initDeviceInstance(DeviceProductEntity product, Authentication auth) {
        DeviceInstanceEntity entity = FastBeanCopier.copy(this, DeviceInstanceEntity.of());

        entity.setProductId(product.getId());
        entity.setProductName(product.getName());

        entity.setCreateTimeNow();
        entity.setCreatorId(auth.getUser().getId());
        entity.setCreatorName(auth.getUser().getName());

        entity.setModifyTimeNow();
        entity.setModifierId(auth.getUser().getId());
        entity.setModifierName(auth.getUser().getName());

        ValidatorUtils.tryValidate(entity, CreateGroup.class);

        this.device = entity;
        return this;
    }

    @Override
    public void fromJson(JSONObject json) {
        Jsonable.super.fromJson(json);

        for (Map.Entry<String, PropertyMetadata> entry : tagMapping.entrySet()) {
            PropertyMetadata maybeTag = entry.getValue();
            if (maybeTag != null) {
                tag(
                    maybeTag.getId(),
                    entry.getKey(),
                    json.getString(maybeTag.getId()),
                    maybeTag.getValueType().getId()
                );
            }
        }

        for (Map.Entry<String, ConfigPropertyMetadata> entry : configMapping.entrySet()) {
            ConfigPropertyMetadata maybeConfig = entry.getValue();
            if (maybeConfig != null) {
                config(
                    maybeConfig.getProperty(),
                    json.getString(maybeConfig.getProperty())
                );
            }
        }
    }
}
