package org.jetlinks.community.rule.engine.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;
import org.jetlinks.rule.engine.api.model.RuleEngineModelParser;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "rule_instance")
public class RuleInstanceEntity extends GenericEntity<String> implements RecordCreationEntity {

    @Override
    @GeneratedValue(generator = "snow_flake")
    public String getId() {
        return super.getId();
    }

    @Column(name = "model_id", length = 32)
    private String modelId;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "model_type")
    private String modelType;

    @Column(name = "model_meta")
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String modelMeta;

    @Column(name = "model_version", nullable = false)
    private Integer modelVersion;

    @Column(name = "create_time")
    private Long createTime;

    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "state")
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("stopped")
    private RuleInstanceState state;

    @Column(name = "instance_detail_json")
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String instanceDetailJson;


    public RuleModel toRule(RuleEngineModelParser parser) {
        RuleModel model = parser.parse(modelType, modelMeta);
        model.setId(StringUtils.hasText(modelId)?modelId:getId());
        model.setName(name);

        return model;
    }
}
