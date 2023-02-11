package org.jetlinks.community.relation.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.utils.DigestUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.jetlinks.community.relation.service.RelatedObjectInfo;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Table(name = "s_object_related", indexes = {
    @Index(name = "idx_rel_obj_key", columnList = "object_key"),
    @Index(name = "idx_rel_obj_t_id", columnList = "object_type,object_id"),
    @Index(name = "idx_rel_rel_key", columnList = "related_key"),
    @Index(name = "idx_rel_relation", columnList = "relation")
})
@EnableEntityEvent
public class RelatedEntity extends GenericEntity<String> {

    @Schema(description = "对象类型",accessMode = Schema.AccessMode.READ_ONLY)
    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String objectType;

    @Schema(description = "对象ID",accessMode = Schema.AccessMode.READ_ONLY)
    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String objectId;

    @Schema(description = "对象KEY,md5(type+|+id)",accessMode = Schema.AccessMode.READ_ONLY)
    @Column(length = 64, nullable = false, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String objectKey;

    @Column(length = 64)
    @Schema(description = "对象名称")
    private String objectName;

    @Schema(description = "目标关系对象类型")
    @Column(length = 32, nullable = false)
    @NotBlank(groups = CreateGroup.class)
    private String relatedType;

    @Schema(description = "目标关系对象ID")
    @Column(length = 64, nullable = false)
    @NotBlank(groups = CreateGroup.class)
    private String relatedId;

    @Schema(description = "目标关系名称")
    @Column(length = 64)
    private String relatedName;

    @Schema(description = "目标关系对象KEY,md5(type+|+id)",accessMode = Schema.AccessMode.READ_ONLY)
    @Column(length = 64, nullable = false)
    @NotBlank(groups = CreateGroup.class)
    private String relatedKey;

    @Schema(description = "关系")
    @Column(length = 32, nullable = false, updatable = false)
    @NotBlank(groups = CreateGroup.class)
    private String relation;

    @Column
    @Schema(description = "创建时间")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    public String getObjectKey() {
        if (!StringUtils.hasText(objectKey)
            && StringUtils.hasText(objectType)
            && StringUtils.hasText(objectId)) {
            objectKey = generateKey(objectType, objectId);
        }
        return objectKey;
    }

    public String getRelatedKey() {
        if (!StringUtils.hasText(relatedKey)
            && StringUtils.hasText(relatedType)
            && StringUtils.hasText(relatedId)) {
            relatedKey = generateKey(relatedType, relatedId);
        }
        return relatedKey;
    }

    public RelatedEntity withObject(String type,String id){
        this.setObjectId(id);
        this.setObjectType(type);
        getObjectKey();
        return this;
    }

    public RelatedEntity withRelated(String type, RelatedObjectInfo info, String relation){
        this.setRelatedId(info.getId());
        this.setRelatedName(info.getName());
        this.setRelatedType(type);
        this.setRelation(relation);
        getRelatedKey();
        return this;
    }


    public static String generateKey(String... keys) {
        return DigestUtils.md5Hex(String.join("|", keys));
    }

    public static Set<String> generateKey(String type, Collection<String> idList) {
        return idList
            .stream()
            .map(id -> RelatedEntity.generateKey(type, id))
            .collect(Collectors.toSet());
    }
}
