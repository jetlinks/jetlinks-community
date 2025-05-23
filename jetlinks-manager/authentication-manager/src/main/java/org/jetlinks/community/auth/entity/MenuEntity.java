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
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.i18n.MultipleI18nSupportEntity;
import org.hswebframework.web.utils.DigestUtils;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 菜单定义实体类
 *
 * @author wangzheng
 * @since 1.0
 */
@Getter
@Setter
@Table(name = "s_menu", indexes = {
    @Index(name = "idx_menu_path", columnList = "path")
})
@Comment("菜单信息表")
@EnableEntityEvent
public class MenuEntity
    extends GenericTreeSortSupportEntity<String> implements RecordCreationEntity, MultipleI18nSupportEntity {

    /**
     * 在多应用集成运行时使用此字段来区分菜单属于哪个系统
     * 具体标识由各应用前端进行定义
     */
    @Schema(description = "菜单所有者")
    @Column(length = 64)
    private String owner;

    @Schema(description = "名称")
    @Column(length = 64, nullable = false)
    @Length(max = 64, min = 1, groups = CreateGroup.class)
    private String name;

    @Schema(description = "编码")
    @Column(length = 64)
    @Length(max = 64, groups = CreateGroup.class)
    private String code;

    @Schema(description = "所属应用")
    @Column(length = 64)
    @Length(max = 64, groups = CreateGroup.class)
    private String application;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "描述")
    private String describe;

    @Column(length = 512)
    @Schema(description = "URL,路由")
    @Length(max = 512, groups = CreateGroup.class)
    private String url;

    @Column(length = 256)
    @Schema(description = "图标")
    @Length(max = 256, groups = CreateGroup.class)
    private String icon;

    @Column
    @ColumnType(jdbcType = JDBCType.SMALLINT)
    @Schema(description = "状态,0为禁用,1为启用")
    @DefaultValue("1")
    private Byte status;

    @Schema(description = "绑定权限信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private List<PermissionInfo> permissions;

    @Schema(description = "按钮定义信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private List<MenuButtonInfo> buttons;

    @Schema(description = "其他配置信息")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private Map<String, Object> options;

    //子菜单
    @Schema(description = "子菜单")
    private List<MenuEntity> children;

    @Column(name = "creator_id", updatable = false)
    @Schema(
        description = "创建者ID(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private String creatorId;

    @Column(name = "create_time", updatable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(
        description = "创建时间(只读)"
        , accessMode = Schema.AccessMode.READ_ONLY
    )
    private Long createTime;

    @Schema(title = "国际化信息定义")
    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.LONGVARCHAR, javaType = String.class)
    private Map<String, Map<String, String>> i18nMessages;


    public String getI18nName() {
        return getI18nMessage("name", name);
    }

    public boolean isSupportDataAccess() {
        return false;
    }

    public MenuEntity copy(Predicate<MenuButtonInfo> buttonPredicate) {
        MenuEntity entity = this.copyTo(new MenuEntity());

        if (CollectionUtils.isEmpty(entity.getButtons())) {
            return entity;
        }
        entity.setButtons(
            entity
                .getButtons()
                .stream()
                .filter(buttonPredicate)
                .collect(Collectors.toList())
        );
        return entity;
    }

    public boolean hasPermission(BiPredicate<String, Collection<String>> predicate) {
        if (CollectionUtils.isEmpty(permissions) && CollectionUtils.isEmpty(buttons)) {
            return false;
        }
        //有权限信息
        if (CollectionUtils.isNotEmpty(permissions)) {
            for (PermissionInfo permission : permissions) {
                if (!predicate.test(permission.getPermission(), permission.getActions())) {
                    return false;
                }
            }
            return true;
        }
        //有任意按钮信息
        if (CollectionUtils.isNotEmpty(buttons)) {
            for (MenuButtonInfo button : buttons) {
                if (button.hasPermission(predicate)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Optional<MenuButtonInfo> getButton(String id) {
        if (buttons == null) {
            return Optional.empty();
        }
        return buttons
            .stream()
            .filter(button -> Objects.equals(button.getId(), id))
            .findAny();
    }

    /**
     * 构建应用的菜单信息
     * 清除菜单ID，用于新增
     *
     * @param appId 应用ID
     * @param owner 所属系统
     * @return 菜单
     */
    public MenuEntity ofApp(String appId,
                            String owner) {
        if (StringUtils.isBlank(getId())) {
            setId(DigestUtils.md5Hex(appId + owner + code));
        }
        setParentId(null);
        setOwner(owner);
        return this;
    }
}
