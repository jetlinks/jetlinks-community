package org.jetlinks.community.rule.engine.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.jetlinks.community.rule.engine.enums.RuleInstanceState;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "rule_model")
public class RuleModelEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Override
    @GeneratedValue(generator = "snow_flake")
    public String getId() {
        return super.getId();
    }

    @Column(name = "name")
    private String name;

    @Column(name = "type")
    private String type;

    @Column(name = "description")
    private String description;

    @Column(name = "model_type",nullable = false)
    private String modelType;

    @Column(name = "model_meta")
    @ColumnType(jdbcType = JDBCType.CLOB)
    private String modelMeta;

    @Column(name = "version",nullable = false)
    private Integer version;

    @Column(name = "creator_id",updatable = false)
    private String creatorId;

    @Column(name = "create_time",updatable = false)
    private Long createTime;

    @Column(name = "modifier_id")
    private String modifierId;

    @Column(name = "modify_time")
    private Long modifyTime;


    public RuleInstanceEntity toInstance() {
        RuleInstanceEntity instanceEntity = new RuleInstanceEntity();
        // rule-1:1
        instanceEntity.setId(getId().concat("-").concat(String.valueOf(getVersion())));
        instanceEntity.setState(RuleInstanceState.stopped);
        instanceEntity.setModelId(getId());
        instanceEntity.setCreateTimeNow();
        instanceEntity.setDescription(getDescription());
        instanceEntity.setName(getName());
        instanceEntity.setModelVersion(getVersion());
        instanceEntity.setModelMeta(getModelMeta());
        instanceEntity.setModelType(getModelType());


        return instanceEntity;

    }
}
