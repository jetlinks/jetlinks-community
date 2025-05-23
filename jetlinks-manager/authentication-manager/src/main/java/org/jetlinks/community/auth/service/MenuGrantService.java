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
package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.auth.entity.MenuBindEntity;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.service.request.MenuGrantRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@Service
@AllArgsConstructor
public class MenuGrantService {

    private final AuthorizationSettingDetailService settingService;
    private final ReactiveRepository<MenuBindEntity, String> bindRepository;
    private final ReactiveRepository<MenuEntity, String> menuRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 对菜单进行授权
     *
     * @param request 授权请求
     * @return void
     */
    @Transactional
    public Mono<Void> grant(MenuGrantRequest request) {
        Set<String> owner = request.containOwner();
        return Flux
            .concat(
                //先删除原已保存的菜单信息
                deleteMenuBind(request.getTargetType(), request.getTargetId(), owner),
                //保存菜单信息
                bindRepository.save(request.toBindEntities()),
                settingService.clearPermissionUserAuth(request.getTargetType(), request.getTargetId())
            )
            .then()
            ;
    }

    /**
     * 清空菜单授权
     * @param targetType 权限类型
     * @param targetId 权限类型对应的数据ID
     * @param owner 菜单所有者
     * @return void
     */
    public Mono<Void> clearGrant(String targetType, String targetId, Set<String> owner) {
        return this
            .deleteMenuBind(targetType, targetId, owner)
            .then();
    }

    private Mono<Integer> deleteMenuBind(String targetType, String targetId, Set<String> owner) {
        return bindRepository
            .createDelete()
            .where(MenuBindEntity::getTargetKey, MenuBindEntity.generateTargetKey(targetType, targetId))
            .nest()
            //兼容之前数据-未记录owner
            .isNull(MenuBindEntity::getOwner)
            .when(!owner.isEmpty(), d -> d.or(MenuBindEntity::getOwner, TermType.in, owner))
            .end()
            .execute();
    }
}
