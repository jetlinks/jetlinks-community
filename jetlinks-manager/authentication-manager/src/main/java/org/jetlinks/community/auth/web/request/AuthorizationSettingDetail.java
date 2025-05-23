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
package org.jetlinks.community.auth.web.request;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.access.DataAccessConfig;
import org.hswebframework.web.system.authorization.api.entity.ActionEntity;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.api.entity.DataAccessEntity;
import org.hswebframework.web.system.authorization.api.entity.PermissionEntity;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.NotBlank;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限设置详情信息
 *
 * @author zhouhao
 * @since 1.0
 */
@Getter
@Setter
public class AuthorizationSettingDetail {

    /**
     * 设置目标类型(维度)标识,如: org, role
     */
    @NotBlank
    @Schema(description = "权限类型,如: org,openApi")
    private String targetType;

    /**
     * 设置目标.
     */
    @NotBlank
    @Schema(description = "权限类型对应的数据ID")
    private String targetId;

    /**
     * 冲突时是否合并
     */
    @Schema(description = "冲突时是否合并")
    private boolean merge = true;

    /**
     * 冲突时优先级
     */
    @Schema(description = "冲突时合并优先级")
    private int priority = 10;

    /**
     * 权限列表
     */
    @Schema(description = "权限列表")
    private List<PermissionInfo> permissionList;

    public boolean hasPermission(String id, Collection<String> actions) {
        if (CollectionUtils.isEmpty(permissionList)) {
            return false;
        }
        for (PermissionInfo info : permissionList) {
            if (Objects.equals(info.getId(), id)) {
                if (CollectionUtils.isEmpty(actions)) {
                    return true;
                }
                if (CollectionUtils.isNotEmpty(info.getActions())) {
                    if (info.getActions().containsAll(actions)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 授权信息
     */
    @Getter
    @Setter
    @EqualsAndHashCode(of = "id")
    @Generated
    public static class PermissionInfo {

        /**
         * 权限ID
         */
        @NotBlank
        @Schema(description = "权限ID")
        private String id;

        @Schema(description = "权限名称")
        private String name;

        /**
         * 授权操作
         */
        @Schema(description = "允许执行的操作")
        private Set<String> actions;

        /**
         * 字段权限
         */
        @Hidden
        private List<FieldAccess> fieldAccess;

        /**
         * 数据权限
         */
        @Hidden
        private List<DataAccess> dataAccess;

        /**
         * 是否来自外部菜单的权限
         */
        @Hidden
        private boolean external;

        @Schema(description = "额外配置")
        private Map<String, Object> options;

        private PermissionInfo unwrap(AuthorizationSettingEntity entity) {
            this.id = entity.getPermission();
            this.actions = entity.getActions();
            this.fieldAccess = new ArrayList<>();
            this.dataAccess = new ArrayList<>();

            //filed : access
            Map<String, FieldAccess> filedAccessMap = new LinkedHashMap<>();


            //type : access
            Map<String, DataAccess> dataAccessMap = new LinkedHashMap<>();
            if (CollectionUtils.isNotEmpty(entity.getDataAccesses())) {
                for (DataAccessEntity access : entity.getDataAccesses()) {
                    //字段权限
                    if (DataAccessConfig.DefaultType.DENY_FIELDS.equalsIgnoreCase(access.getType())) {
                        Set<String> fields = Optional.ofNullable(access.getConfig())
                                                     .<Set<String>>map(conf -> new HashSet<>((Collection<String>) conf.get("fields")))
                                                     .orElseGet(HashSet::new);

                        for (String field : fields) {
                            filedAccessMap
                                .computeIfAbsent(field, filedName -> new FieldAccess(filedName, new HashSet<>()))
                                .getAction().add(access.getAction());
                        }
                    } else {
                        //维度权限
                        if (DataAccessConfig.DefaultType.DIMENSION_SCOPE.equalsIgnoreCase(access.getType())) {
                            DataAccess dataAccess = DataAccess.of(access);

                            dataAccessMap.computeIfAbsent(dataAccess.getType(), __ -> dataAccess)
                                .actions
                                .add(access.getAction());

                        }
                    }
                }
            }
            this.dataAccess.addAll(dataAccessMap.values());
            this.fieldAccess.addAll(filedAccessMap.values());

            return this;
        }

        private void wrap(AuthorizationSettingEntity entity) {
            entity.setPermission(id);
            entity.setActions(actions);
            List<DataAccessEntity> entities = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(fieldAccess)) {
                Map<String, Set<String>> group = new HashMap<>();
                for (FieldAccess access : fieldAccess) {
                    for (String action : access.getAction()) {
                        group.computeIfAbsent(action, r -> new HashSet<>())
                             .add(access.name);
                    }
                }
                for (Map.Entry<String, Set<String>> entry : group.entrySet()) {
                    DataAccessEntity dataAccessEntity = new DataAccessEntity();
                    dataAccessEntity.setAction(entry.getKey());
                    dataAccessEntity.setType(DataAccessConfig.DefaultType.DENY_FIELDS);
                    dataAccessEntity.setConfig(Collections.singletonMap("fields", entry.getValue()));
                    entities.add(dataAccessEntity);
                }
            }

            if (CollectionUtils.isNotEmpty(dataAccess)) {
                for (DataAccess access : dataAccess) {
                    entities.addAll(access.toEntity());
                }
            }
            entity.setDataAccesses(entities);
        }

        public PermissionEntity toPermissionEntity() {
            PermissionEntity permissionEntity = new PermissionEntity();
            permissionEntity.setId(id);
            permissionEntity.setName(StringUtils.hasText(name) ? name : id);
            List<ActionEntity> actionEntityList = getActions()
                .stream()
                .map(action -> {
                    ActionEntity actionEntity = new ActionEntity();
                    actionEntity.setAction(action);
                    return actionEntity;
                })
                .collect(Collectors.toList());

            permissionEntity.setActions(actionEntityList);
            return permissionEntity;
        }

        public static PermissionInfo of(String id, Collection<String> actions) {
            PermissionInfo info = new PermissionInfo();
            info.setId(id);
            info.setActions(new HashSet<>(actions));
            return info;
        }
    }


    /**
     * 数据权限
     */
    @Getter
    @Setter
    @Generated
    public static class DataAccess {

        /**
         * 维度类型,如: org
         */
        private String type;

        /**
         * 操作
         */
        private Set<String> actions;

        /**
         * 其他配置
         */
        private Map<String, Object> config;

        public static DataAccess of(DataAccessEntity entity) {
            DataAccess access = new DataAccess();
            access.config = entity.getConfig();
            access.actions = new HashSet<>();
            access.type = (String) access.getConfig().get("scopeType");
            return access;
        }

        public List<DataAccessEntity> toEntity() {
            if (CollectionUtils.isEmpty(actions)) {
                return Collections.emptyList();
            }
            return actions
                .stream()
                .map(action -> {
                    DataAccessEntity entity = new DataAccessEntity();
                    entity.setType(DataAccessConfig.DefaultType.DIMENSION_SCOPE);
                    Map<String, Object> config = new HashMap<>();
                    if (null != this.config) {
                        config.putAll(this.config);
                    }
                    config.put("scopeType", type);
                    config.put("children", true);
                    entity.setConfig(config);
                    entity.setAction(action);
                    return entity;
                }).collect(Collectors.toList());
        }
    }

    /**
     * 字段权限,控制指定操作不能访问指定的字段.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Generated
    public static class FieldAccess {

        /**
         * 字段名
         */
        private String name;

        /**
         * 操作
         */
        private Set<String> action;

    }

    public static AuthorizationSettingDetail fromEntity(List<AuthorizationSettingEntity> entities) {
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setPermissionList(new ArrayList<>());

        for (AuthorizationSettingEntity entity : entities) {
            detail.setTargetId(entity.getDimensionTarget());
            detail.setTargetType(entity.getDimensionType());
            detail.getPermissionList().add(new PermissionInfo().unwrap(entity));
        }

        return detail;
    }

    public List<AuthorizationSettingEntity> toEntity() {
        return toEntity(null);
    }

    public List<AuthorizationSettingEntity> toEntity(Dimension dimension) {
        if (CollectionUtils.isEmpty(permissionList)) {
            return Collections.emptyList();
        }
        return permissionList
            .stream()
            .filter(permissionInfo -> CollectionUtils.isNotEmpty(permissionInfo.actions))
            .map(permissionInfo -> {
                AuthorizationSettingEntity entity = new AuthorizationSettingEntity();
                entity.setDimensionTarget(targetId);
                entity.setDimensionType(targetType);
                if (null != dimension) {
                    entity.setDimensionTypeName(dimension.getType().getName());
                    entity.setDimensionTargetName(dimension.getName());
                }
                entity.setState((byte) 1);
                entity.setMerge(merge);
                entity.setPriority(priority);
                permissionInfo.wrap(entity);
                return entity;
            }).collect(Collectors.toList());
    }
}
