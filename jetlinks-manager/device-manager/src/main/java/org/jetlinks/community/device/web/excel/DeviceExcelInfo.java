package org.jetlinks.community.device.web.excel;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.reactor.excel.CellDataType;
import org.hswebframework.reactor.excel.ExcelHeader;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.entity.DeviceTagEntity;
import org.jetlinks.core.metadata.ConfigPropertyMetadata;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
public class DeviceExcelInfo implements Jsonable {

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "设备ID")
    @NotBlank(message = "设备ID不能为空")
    private String id;

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "设备名称")
    @NotBlank(message = "设备名称不能为空")
    private String name;

    private String orgName;

    private String productName;

    @org.jetlinks.community.io.excel.annotation.ExcelHeader(value = "父设备ID")
    private String parentId;

    private List<DeviceTagEntity> tags = new ArrayList<>();

    private DeviceInstanceEntity device;

    private Map<String, PropertyMetadata> tagMapping;

    private Map<String, ConfigPropertyMetadata> configMapping;

    private Map<String, Object> configuration = new HashMap<>();

    private long rowNumber;

    private String state;

    public void config(String key, Object value) {
        if (value == null || value instanceof String && !StringUtils.hasText((String) value)) {
            return;
        }
        configuration.put(key, value);
    }

    public void tag(String key, String name, Object value, String type) {
        if (value == null) {
            return;
        }
        DeviceTagEntity entity = new DeviceTagEntity();
        entity.setKey(key);
        entity.setValue(String.valueOf(value));
        entity.setName(name);
        entity.setDeviceId(id);
        entity.setType(type);
        entity.setId(DeviceTagEntity.createTagId(id,key));
        tags.add(entity);
    }

    public void setId(String id) {
        this.id = id;
        for (DeviceTagEntity tag : tags) {
            tag.setDeviceId(id);
            tag.setId(DeviceTagEntity.createTagId(tag.getDeviceId(),tag.getKey()));
        }
    }

    public void with(String key, Object value) {
        FastBeanCopier.copy(Collections.singletonMap(key, value), this);
    }

    public Map<String,Object> toMap(){
        Map<String,Object> val = FastBeanCopier.copy(this,new HashMap<>());
        for (DeviceTagEntity tag : tags) {
            val.put(tag.getKey(),tag.getValue());
        }
        return val;
    }

    public static List<ExcelHeader> getTemplateHeaderMapping(DeviceExcelFilterColumns filterColumns,
                                                             List<PropertyMetadata> tags,
                                                             List<ConfigPropertyMetadata> configs) {
        List<ExcelHeader> arr =
            Arrays.stream(new ExcelHeader[]{
                    new ExcelHeader("id", "设备ID", CellDataType.STRING),
                    new ExcelHeader("name", "设备名称", CellDataType.STRING),
                    new ExcelHeader("parentId", "父设备ID", CellDataType.STRING)
                })
                .filter(a-> !filterColumns.getColumns().contains(a.getKey()))
                .collect(Collectors.toList());
        for (PropertyMetadata tag : tags) {
            arr.add(new ExcelHeader(tag.getId(), StringUtils.isEmpty(tag.getName()) ? tag.getId() : tag.getName(), CellDataType.STRING));
        }

        for (ConfigPropertyMetadata config : configs) {
            arr.add(new ExcelHeader("configuration." + config.getProperty(), StringUtils.isEmpty(config.getName()) ? config
                .getProperty() : config.getName(), CellDataType.STRING));
        }
        return arr;
    }

    public static List<ExcelHeader> getExportHeaderMapping(DeviceExcelFilterColumns filterColumns,
                                                           List<PropertyMetadata> tags,
                                                           List<ConfigPropertyMetadata> configs) {
        List<ExcelHeader> arr =
            Arrays.stream(new ExcelHeader[]{
                    new ExcelHeader("id", "设备ID", CellDataType.STRING),
                    new ExcelHeader("name", "设备名称", CellDataType.STRING),
                    new ExcelHeader("productName", "产品名称", CellDataType.STRING),
                    new ExcelHeader("parentId", "父设备ID", CellDataType.STRING),
                    new ExcelHeader("state", "状态", CellDataType.STRING)
                })
                .filter(a-> !filterColumns.getColumns().contains(a.getKey()))
                .collect(Collectors.toList());

        for (PropertyMetadata tag : tags) {
            arr.add(new ExcelHeader(tag.getId(), StringUtils.isEmpty(tag.getName()) ? tag.getId() : tag.getName(), CellDataType.STRING));
        }
        for (ConfigPropertyMetadata config : configs) {
            arr.add(new ExcelHeader("configuration." + config.getProperty(),
                StringUtils.isEmpty(config.getName()) ? config.getProperty() : config.getName(),
                CellDataType.STRING));
        }
        return arr;
    }

    public static Map<String, String> getImportHeaderMapping() {
        Map<String, String> mapping = new HashMap<>();

        mapping.put("设备ID", "id");
        mapping.put("设备名称", "name");
        mapping.put("名称", "name");

//        mapping.put("所属机构", "orgName");
        mapping.put("父设备ID", "parentId");

        return mapping;
    }

    public DeviceExcelInfo initDeviceInstance(DeviceProductEntity product, Authentication auth) {
        DeviceInstanceEntity entity = FastBeanCopier.copy(this, new DeviceInstanceEntity());

        entity.setProductId(product.getId());
        entity.setProductName(product.getName());

        entity.setCreateTimeNow();
        entity.setCreatorId(auth.getUser().getId());
        entity.setCreatorName(auth.getUser().getName());

        entity.setModifyTimeNow();
        entity.setModifierId(auth.getUser().getId());
        entity.setModifierName(auth.getUser().getName());

        ValidatorUtils.tryValidate(entity);

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
                    Optional.ofNullable(json.getString(maybeTag.getId())).orElse(null),
                    maybeTag.getValueType().getId()
                );
            }
        }

        for (Map.Entry<String, ConfigPropertyMetadata> entry : configMapping.entrySet()) {
            ConfigPropertyMetadata maybeConfig = entry.getValue();
            if (maybeConfig != null) {
                config(
                    maybeConfig.getProperty(),
                    Optional.ofNullable(json.getString(maybeConfig.getProperty())).orElse(null)
                );
            }
        }
    }
}
