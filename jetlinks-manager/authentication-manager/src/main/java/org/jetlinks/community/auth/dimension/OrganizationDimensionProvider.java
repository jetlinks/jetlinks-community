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
package org.jetlinks.community.auth.dimension;

import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.hswebframework.web.system.authorization.defaults.service.terms.DimensionTerm;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.service.OrganizationService;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrganizationDimensionProvider extends BaseDimensionProvider<OrganizationEntity> {

    private final OrganizationService organizationService;

    public OrganizationDimensionProvider(OrganizationService organizationService,
                                         DefaultDimensionUserService dimensionUserService,
                                         ApplicationEventPublisher eventPublisher) {
        super(organizationService.getRepository(), eventPublisher, dimensionUserService);
        this.organizationService = organizationService;
    }

    @Override
    protected DimensionType getDimensionType() {
        return OrgDimensionType.org;
    }

    @Override
    protected Mono<Dimension> convertToDimension(OrganizationEntity entity) {
        return Mono.just(entity.toDimension(true));
    }

    @Override
    public Flux<? extends Dimension> getDimensionByUserId(String s) {
        Map<String, Dimension> dimensions = new LinkedHashMap<>();

        return DimensionTerm
            .inject(createQuery(), "id", getDimensionType().getId(), Collections.singletonList(s))
            .fetch()
            //直接关联的部门
            .doOnNext(org -> dimensions.put(org.getId(), org.toDimension(true)))
            .concatMap(e -> ObjectUtils.isEmpty(e.getPath())
                ? Mono.just(e)
                : createQuery()
                .where()
                //使用path快速查询
                .like$("path", e.getPath())
                .fetch()
                //子级部门
                .doOnNext(org -> dimensions.putIfAbsent(org.getId(), org.toDimension(false)))
            )
            .thenMany(Flux.defer(() -> Flux.fromIterable(dimensions.values())));
    }

    @Override
    protected Mono<Void> clearUserAuthenticationCache(Collection<OrganizationEntity> entities) {
        //清空上下级,因为维度中记录了上下级信息.
        return Flux.concat(
                       organizationService.queryIncludeParent(Flux.fromIterable(entities)),
                       organizationService.queryIncludeChildren(Flux.fromIterable(entities))
                   )
                   .distinct(OrganizationEntity::getId)
                   .collectList()
                   .flatMap(super::clearUserAuthenticationCache)
                   .as(ClearUserAuthorizationCacheEvent::doOnEnabled);
    }


    @EventListener
    public void handleCreatEvent(EntityCreatedEvent<OrganizationEntity> event) {
        event.async(
            clearUserAuthenticationCache(event.getEntity())
        );
    }

}
