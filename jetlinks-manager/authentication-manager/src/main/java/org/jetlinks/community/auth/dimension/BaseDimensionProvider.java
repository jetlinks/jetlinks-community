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

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.utils.TransactionUtils;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.hswebframework.web.system.authorization.defaults.service.terms.DimensionTerm;
import org.jetlinks.community.auth.utils.DimensionUserBindUtils;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.GenericTypeResolver;
import org.springframework.transaction.reactive.TransactionSynchronization;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public abstract class BaseDimensionProvider<T extends GenericEntity<String>> implements DimensionProvider {

    protected final ReactiveRepository<T, String> repository;

    protected final ApplicationEventPublisher eventPublisher;

    protected final DefaultDimensionUserService dimensionUserService;

    private Class<?> entityType;

    protected abstract DimensionType getDimensionType();

    protected abstract Mono<Dimension> convertToDimension(T entity);

    protected ReactiveQuery<T> createQuery() {
        return repository.createQuery();
    }

    @Override
    public Flux<? extends Dimension> getDimensionByUserId(String s) {
        return DimensionTerm
            .inject(createQuery(), "id", getDimensionType().getId(), Collections.singletonList(s))
            .fetch()
            .as(this::convertToDimension);
    }

    @Override
    public Mono<? extends Dimension> getDimensionById(DimensionType dimensionType, String s) {
        if (!dimensionType.isSameType(getDimensionType())) {
            return Mono.empty();
        }
        return repository
            .findById(s)
            .as(this::convertToDimension)
            .singleOrEmpty();
    }

    @Override
    public Flux<? extends Dimension> getDimensionsById(DimensionType type, Collection<String> idList) {
        if (!type.isSameType(getDimensionType())) {
            return Flux.empty();
        }
        return repository
            .findById(idList)
            .as(this::convertToDimension);
    }

    protected Flux<? extends Dimension> convertToDimension(Publisher<T> source) {
        return Flux.from(source).flatMap(this::convertToDimension,8);
    }

    @Override
    public Flux<String> getUserIdByDimensionId(String s) {
        return dimensionUserService
            .createQuery()
            .where(DimensionUserEntity::getDimensionId, s)
            .and(DimensionUserEntity::getDimensionTypeId, getDimensionType().getId())
            .fetch()
            .map(DimensionUserEntity::getUserId);
    }

    @Override
    public Flux<? extends DimensionType> getAllType() {
        return Flux.just(getDimensionType());
    }

    protected Class<?> getEntityType() {
        return entityType == null
            ? entityType = GenericTypeResolver.resolveTypeArgument(this.getClass(), BaseDimensionProvider.class)
            : entityType;
    }

    private boolean isNotSameType(Class<?> type) {
        Class<?> genType = getEntityType();

        return genType == null || !genType.isAssignableFrom(type);
    }

    @EventListener
    public void handleEvent(EntityDeletedEvent<T> event) {
        if (isNotSameType(event.getEntityType())) {
            return;
        }
        // 删除绑定信息
        event.async(
            DimensionUserBindUtils.unbindUser(
                dimensionUserService,
                null,
                getDimensionType().getId(),
                Lists.transform(event.getEntity(), GenericEntity::getId))
//            clearUserAuthenticationCache(event.getEntity())
        );
    }

    @EventListener
    public void handleEvent(EntitySavedEvent<T> event) {
        if (isNotSameType(event.getEntityType())) {
            return;
        }
        event.async(
            clearUserAuthenticationCache(event.getEntity())
        );
    }

    @EventListener
    public void handleEvent(EntityModifyEvent<T> event) {
        if (isNotSameType(event.getEntityType())) {
            return;
        }
        Map<String, T> beforeMap = event
            .getBefore()
            .stream()
            .collect(Collectors.toMap(T::getId, Function.identity()));

        List<T> readyToClear = event
            .getAfter()
            .stream()
            .filter(after -> isChanged(beforeMap.get(after.getId()), after))
            .collect(Collectors.toList());

        if (readyToClear.isEmpty()) {
            return;
        }
        event.async(
            clearUserAuthenticationCache(readyToClear)
        );
    }

    protected boolean isChanged(T before, T after) {
        return true;
    }

    @SuppressWarnings("all")
    private Mono<Void> clearUserAuthenticationCache0(Collection<String> idList) {
        return Flux
            .fromIterable(Collections2.filter(idList, Objects::nonNull))
            .buffer(200)
            .flatMap(list -> dimensionUserService
                .createQuery()
                .where()
                .select(DimensionUserEntity::getUserId)
                .in(DimensionUserEntity::getDimensionId, list)
                .and(DimensionUserEntity::getDimensionTypeId, getDimensionType().getId())
                .fetch()
                .map(DimensionUserEntity::getUserId)
                .collect(Collectors.toSet())
                .filter(CollectionUtils::isNotEmpty)
                .flatMap(users -> ClearUserAuthorizationCacheEvent.of(users).publish(eventPublisher)))
            .then();
    }

    protected Mono<Void> clearUserAuthenticationCacheById(Collection<String> entities) {
        return ClearUserAuthorizationCacheEvent
            .doOnEnabled(
                TransactionUtils
                    .registerSynchronization(new TransactionSynchronization() {
                        @Override
                        @Nonnull
                        public Mono<Void> afterCommit() {
                            return clearUserAuthenticationCache0(entities);
                        }
                    }, TransactionSynchronization::afterCommit)
            );
    }

    protected Mono<Void> clearUserAuthenticationCache(Collection<T> entities) {
        return clearUserAuthenticationCacheById(Collections2.transform(entities, GenericEntity::getId));
    }

}
