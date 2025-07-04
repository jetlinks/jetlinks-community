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
package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.EntityFactoryHolder;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.utils.DigestUtils;
import org.springframework.util.ObjectUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import java.sql.JDBCType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@Table(name = "s_menu_bind", indexes = {
    @Index(name = "idx_menu_bind_dim_key", columnList = "target_key")
})
@Comment("菜单绑定信息表")
public class MenuBindEntity extends GenericEntity<String> {

    @Schema(description = "绑定维度类型,比如role,user")
    @Column(nullable = false, length = 32, updatable = false)
    @NotBlank
    private String targetType;

    @Schema(description = "绑定维度ID")
    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank
    private String targetId;

    @Schema(description = "绑定key", hidden = true)
    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank
    private String targetKey;

    @Schema(description = "菜单ID")
    @Column(nullable = false, length = 64, updatable = false)
    @NotBlank
    private String menuId;

    /**
     * @since 2.2 此前数据未记录
     */
    @Schema(description = "菜单拥有者")
    @Column(length = 64, updatable = false)
    @NotBlank
    private String owner;

    @Schema(description = "其他配置")
    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @JsonCodec
    private Map<String, Object> options;

    @Schema(description = "分配的按钮")
    @Column
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    @JsonCodec
    private List<MenuView.ButtonView> buttons;

    @Schema(description = "冲突时是否合并")
    @Column
    @DefaultValue("true")
    private Boolean merge;

    @Schema(description = "冲突时合并优先级")
    @Column
    @DefaultValue("10")
    private Integer priority;

    @Override
    public String getId() {
        if (ObjectUtils.isEmpty(super.getId())) {
            generateId();
        }
        return super.getId();
    }

    public void generateId() {
        generateTargetKey();
        if (StringUtils.isNotEmpty(targetKey)){
            setId(DigestUtils.md5Hex(String.join("|", targetKey, menuId)));
        }
    }

    public void generateTargetKey() {
        if (StringUtils.isNotEmpty(targetId) && StringUtils.isNotEmpty(targetType)) {
            setTargetKey(generateTargetKey(targetType, targetId));
        }
    }

    public static String generateTargetKey(String dimensionType, String dimensionId) {
        return DigestUtils.md5Hex(String.join("|", dimensionType, dimensionId));
    }

    public MenuBindEntity withTarget(String targetType, String targetId) {
        this.targetId = targetId;
        this.targetType = targetType;
        generateTargetKey();
        return this;
    }

    public MenuBindEntity withMerge(Boolean merge, Integer priority) {
        this.merge = merge;
        this.priority = priority;
        return this;
    }

    public static MenuBindEntity create() {
        return EntityFactoryHolder.newInstance(MenuBindEntity.class, MenuBindEntity::new);
    }


    public static MenuBindEntity of(MenuView view) {
        MenuBindEntity entity = FastBeanCopier.copy(view, create(), "id");
        entity.setMenuId(view.getId());
        entity.setOptions(view.getOptions());
        entity.setOwner(view.getOwner());

        if (CollectionUtils.isNotEmpty(view.getButtons())) {
            //只保存已经授权的按钮
            entity.setButtons(view.getButtons()
                                  .stream()
                                  .filter(MenuView.ButtonView::isGranted)
                                  .collect(Collectors.toList()));
        }

        return entity;
    }
}
