package org.jetlinks.community.notify.manager.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.VariableDefinition;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;

/**
 * @author wangzheng
 * @author zhouhao
 * @since 1.0
 */
@Setter
@Getter
@Table(name = "notify_template")
@Comment("消息通知模板表")
@EnableEntityEvent
public class NotifyTemplateEntity extends GenericEntity<String> implements RecordCreationEntity {
    private static final long serialVersionUID = -6849794470754667710L;

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(regexp = "^[0-9a-zA-Z_\\-]+$", groups = CreateGroup.class)
    @Schema(description = "模板ID(只能由数字,字母,下划线和中划线组成)")
    public String getId() {
        return super.getId();
    }

    @Column
    @Schema(description = "通知类型ID")
    @NotBlank(groups = CreateGroup.class)
    private String type;

    @Column
    @Schema(description = "通知服务商ID")
    @NotBlank(groups = CreateGroup.class)
    private String provider;

    @Column
    @Schema(description = "模版名称")
    private String name;

    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR,javaType = String.class)
    @JsonCodec
    @Schema(description = "模版内容(根据服务商不同而不同)")
    private Map<String,Object> template;

    @Column(updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @JsonCodec
    @Schema(description = "变量定义")
    private List<VariableDefinition> variableDefinitions;

    //绑定通知配置ID,表示模版必须使用指定的配置进行发送
    @Column(length = 64)
    @Schema(description = "通知配置ID")
    private String configId;

    @Column
    @Schema(description = "说明")
    private String description;

    public TemplateProperties toTemplateProperties() {
        TemplateProperties properties = new TemplateProperties();
        properties.setId(getId());
        properties.setProvider(provider);
        properties.setType(type);
        properties.setConfigId(configId);
        properties.setName(name);
        properties.setTemplate(template);
        properties.setVariableDefinitions(variableDefinitions);
        properties.setDescription(description);
        return properties;
    }
}
