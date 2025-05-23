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
package org.jetlinks.community.auth.initialize;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.simple.SimplePermission;
import org.hswebframework.web.system.authorization.api.entity.ActionEntity;
import org.hswebframework.web.system.authorization.api.entity.PermissionEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultPermissionService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author gyl
 * @since 2.2
 */
@Component
public class PermissionCacheHelper {

    @Getter
    private final Mono<Map<String, PermissionEntity>> permissionCaching;

    public PermissionCacheHelper(DefaultPermissionService permissionService) {
        this.permissionCaching =
            Mono.defer(() -> permissionService
                    .createQuery()
                    .where(PermissionEntity::getStatus, 1)
                    .fetch()
                    .collectMap(PermissionEntity::getId, Function.identity(), LinkedHashMap::new))
                // FIXME: 2023/11/14 PermissionEntity不会频繁变更
                .cache(Duration.ofSeconds(1));

    }


    /**
     * 根据权限实体id过滤对应操作
     *
     * @param permissionId 权限id
     * @param perActions   目标操作
     * @return Permission（操作为实体操作与目标操作的交集）
     */
    public Mono<Permission> filterByDefaultPermission(String permissionId, Set<String> perActions) {
        return permissionCaching
            .mapNotNull(map -> filterByDefaultPermission(map.get(permissionId), perActions));
    }

    /**
     * 根据权限实体过滤对应操作
     *
     * @param entity     权限实体
     * @param perActions 目标操作
     * @return Permission（操作为实体操作与目标操作的交集）
     */
    public static Permission filterByDefaultPermission(PermissionEntity entity, Set<String> perActions) {
        if (entity == null || CollectionUtils.isEmpty(perActions)) {
            return null;
        }

        Set<String> actions;
        if (CollectionUtils.isEmpty(entity.getActions())) {
            actions = new HashSet<>();
        } else {
            Set<String> defActions = entity
                .getActions()
                .stream()
                .map(ActionEntity::getAction)
                .collect(Collectors.toSet());
            actions = new HashSet<>(perActions);
            actions.retainAll(defActions);
        }

        return SimplePermission
            .builder()
            .id(entity.getId())
            .name(entity.getName())
            .options(entity.getProperties())
            .actions(actions)
            .build();
    }
}
