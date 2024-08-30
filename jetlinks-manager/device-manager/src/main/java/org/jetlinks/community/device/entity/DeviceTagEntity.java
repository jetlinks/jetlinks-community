package org.jetlinks.community.device.entity;


import com.alibaba.fastjson.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.ArrayType;
import org.jetlinks.core.metadata.types.DataTypes;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.core.metadata.types.UnknownType;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
@Setter
@Table(name = "dev_device_tags", indexes = {
    @Index(name = "dev_dev_id_idx", columnList = "device_id"),
    @Index(name = "dev_tag_idx", columnList = "device_id,key,value")
})
@EnableEntityEvent
public class DeviceTagEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank(message = "[deviceId]不能为空", groups = CreateGroup.class)
    @Schema(description = "设备ID")
    private String deviceId;

    @Column(length = 64, updatable = false, nullable = false)
    @NotBlank(message = "[key]不能为空", groups = CreateGroup.class)
    @Schema(description = "标签标识")
    private String key;

    @Column
    @Schema(description = "标签名称")
    private String name;

    @Column(length = 256, nullable = false)
    @NotNull(message = "[value]不能为空", groups = CreateGroup.class)
    @Length(max = 256, message = "[value]长度不能大于256", groups = CreateGroup.class)
    @Schema(description = "标签值")
    private String value;

    @Column(length = 32, nullable = false)
    @DefaultValue("string")
    @Schema(description = "类型", defaultValue = "string")
    private String type;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "创建时间(只读)")
    private Date createTime;

    @Column
    @Schema(description = "说明")
    private String description;

    private DataType dataType;

    public static DeviceTagEntity of(PropertyMetadata property) {
        DeviceTagEntity entity = new DeviceTagEntity();
        entity.setKey(property.getId());
        entity.setName(property.getName());
        entity.setType(property.getValueType().getId());
        entity.setDescription(property.getDescription());
        entity.setCreateTime(new Date());
        entity.setDataType(property.getValueType());
        return entity;
    }

    public static DeviceTagEntity of(PropertyMetadata property, Object value) {
        DeviceTagEntity tag = of(property);

        DataType type = property.getValueType();
        if (type instanceof Converter) {
            Object newValue = ((Converter<?>) type).convert(value);
            if (newValue != null) {
                value = newValue;
            }
        }

        String stringValue;
        switch (type.getId()) {
            //结构体和数组类型转为json字符串
            case ObjectType.ID:
            case ArrayType.ID:
                stringValue = JSON.toJSONString(value);
                break;
            default:
                stringValue = String.valueOf(value);
        }

        tag.setValue(stringValue);
        return tag;
    }

    public DeviceProperty toProperty() {
        DeviceProperty property = new DeviceProperty();
        property.setProperty(getKey());
        property.setDeviceId(deviceId);
        property.setType(type);
        property.setPropertyName(name);
        DataType type = Optional
            .ofNullable(DataTypes.lookup(getType()))
            .map(Supplier::get)
            .orElseGet(UnknownType::new);
        if (type instanceof Converter) {
            property.setValue(((Converter<?>) type).convert(getValue()));
        } else {
            property.setValue(getValue());
        }
        return property;


    }

    //以物模型标签基础数据为准，重构数据库保存的可能已过时的标签数据
    public DeviceTagEntity restructure(DeviceTagEntity tag) {
        this.setDataType(tag.getDataType());
        this.setName(tag.getName());
        this.setType(tag.getType());
        this.setKey(tag.getKey());
        this.setDescription(tag.getDescription());
        return this;
    }

    public void generateId() {
        setId(createTagId(deviceId, key));
    }

    public static String createTagId(String deviceId, String key) {
        return DigestUtils.md5Hex(deviceId + ":" + key);
    }

    public static Set<String> parseTagKey(String metadata) {
        return JetLinksDeviceMetadataCodec
            .getInstance()
            .doDecode(metadata)
            .getTags()
            .stream()
            .map(PropertyMetadata::getId)
            .collect(Collectors.toSet());
    }

    public static Set<String> parseTagKey(DeviceMetadata metadata) {
        return metadata
            .getTags()
            .stream()
            .map(PropertyMetadata::getId)
            .collect(Collectors.toSet());
    }
}
