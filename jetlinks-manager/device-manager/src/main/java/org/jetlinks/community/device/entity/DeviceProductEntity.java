package org.jetlinks.community.device.entity;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.hswebframework.web.validator.UpdateGroup;
import org.jetlinks.community.device.enums.DeviceType;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_product")
public class DeviceProductEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    public String getId() {
        return super.getId();
    }

    @Comment("名称")
    @Column(name = "name")
    @NotBlank(message = "产品名称不能为空",groups = CreateGroup.class)
    private String name;

    @Comment("所属项目")
    @Column(name = "project_id",length = 32)
    private String projectId;

    @Comment("项目名称")
    @Column(name = "project_name")
    private String projectName;

    @Comment("说明")
    @Column(name = "describe")
    private String describe;

    @Comment("分类ID")
    @Column(name = "classified_id")
    private String classifiedId;

    @Comment("消息协议: Alink,JetLinks")
    @Column(name = "message_protocol")
    @NotBlank(message = "消息协议不能为空",groups = CreateGroup.class)
    @Length(min = 1,max = 256,groups = {
            CreateGroup.class, UpdateGroup.class
    })
    private String messageProtocol;

    @Comment("协议元数据")
    @Column(name = "metadata")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @NotBlank(message = "元数据不能为空",groups = CreateGroup.class)
    private String metadata;

    @Comment("传输协议: MQTT,COAP,UDP")
    @Column(name = "transport_protocol")
    private String transportProtocol;

    @Comment("入网方式: 直连,组网...")
    @Column(name = "network_way")
    private String networkWay;

    @Comment("设备类型: 网关，设备")
    @Column(name = "device_type")
    @ColumnType(javaType =String.class )
    @EnumCodec
    private DeviceType deviceType;

    @Comment("配置")
    @Column(name = "configuration")
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB)
    private Map<String, Object> configuration;

    @Comment("产品状态")
    @Column(name = "state")
    @DefaultValue("0")
    private Byte state;

    @Column(name = "creator_id")
    @Comment("创建者id")
    private String creatorId;

    @Comment("创建时间")
    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "org_id", length = 32)
    @Comment("所属机构id")
    private String orgId;

}
