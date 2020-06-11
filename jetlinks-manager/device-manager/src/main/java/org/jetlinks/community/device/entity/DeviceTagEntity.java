package org.jetlinks.community.device.entity;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.core.metadata.PropertyMetadata;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.util.Date;

@Getter
@Setter
@Table(name = "dev_device_tags", indexes = {
    @Index(name = "dev_dev_id_idx", columnList = "device_id"),
    @Index(name = "dev_tag_idx", columnList = "device_id,key,value")
})
public class DeviceTagEntity extends GenericEntity<String> {

    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(message = "[deviceId]不能为空")
    private String deviceId;

    @Column(length = 32, updatable = false, nullable = false)
    @NotBlank(message = "[key]不能为空")
    private String key;

    @Column
    private String name;

    @Column(length = 128, nullable = false)
    @NotBlank(message = "[value]不能为空")
    private String value;

    @Column(length = 32, nullable = false)
    @DefaultValue("string")
    private String type;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Date createTime;

    @Column
    private String description;

    public static DeviceTagEntity of(PropertyMetadata property){
        DeviceTagEntity entity=new DeviceTagEntity();
        entity.setKey(property.getId());
        entity.setName(property.getName());
        entity.setType(property.getValueType().getId());
        entity.setDescription(property.getDescription());
        entity.setCreateTime(new Date());
        return entity;
    }

    public static String createTagId(String deviceId,String key){
        return DigestUtils.md5Hex(deviceId + ":" + key);
    }
}
