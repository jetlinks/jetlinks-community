package org.jetlinks.community.auth.service;

import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeUtils;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.service.info.OrgPositionUnbindInfo;
import org.jetlinks.community.auth.utils.DimensionUserBindUtils;
import org.jetlinks.community.auth.web.response.OrganizationDetail;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.jetlinks.core.event.EventBus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service
@AllArgsConstructor
public class OrganizationService extends GenericReactiveTreeSupportCrudService<OrganizationEntity, String>
    implements CommandLineRunner {

    private DefaultDimensionUserService dimensionUserService;

    private final EventBus eventBus;

    private QueryHelper queryHelper;

    private final Map<String, OrganizationEntity> inMemoryCache = new ConcurrentHashMap<>();


    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.UUID;
    }

    @Override
    public void setChildren(OrganizationEntity entity, List<OrganizationEntity> children) {
        entity.setChildren(children);
    }

    @Transactional
    public Mono<Integer> bindUser(String orgId, List<String> userIdList) {
        Flux<String> userIdStream = Flux.fromIterable(userIdList);

        return this
            .findById(orgId)
            .flatMap(org -> userIdStream
                .map(userId -> {
                    DimensionUserEntity userEntity = new DimensionUserEntity();
                    userEntity.setUserId(userId);
                    userEntity.setUserName(userId);
                    userEntity.setDimensionId(orgId);
                    userEntity.setDimensionTypeId(OrgDimensionType.org.getId());
                    userEntity.setDimensionName(org.getName());
                    return userEntity;
                })
                .as(dimensionUserService::save))
            .map(SaveResult::getTotal);

    }

    @Transactional
    public Mono<Integer> unbindUser(@NotNull List<String> userIdList,
                                    @Nullable List<String> orgIdList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Mono.empty();
        }

        return eventBus
            .publish("/unbind/user/position", OrgPositionUnbindInfo.of(userIdList, orgIdList))
            .then(unbindUserByOrganization(userIdList, orgIdList));
    }

    public Mono<Integer> unbindUserByOrganization(@NotNull List<String> userIdList,
                                                  @Nullable List<String> orgIdList) {
        return DimensionUserBindUtils
            .unbindUser(dimensionUserService, userIdList, OrgDimensionType.org.getId(), orgIdList);
    }

    @Transactional
    public Mono<Integer> unbindAllUser(@NotNull List<String> orgIdList) {
        if (CollectionUtils.isEmpty(orgIdList)) {
            return Mono.empty();
        }
        return DimensionUserBindUtils
            .unbindUser(dimensionUserService, null, OrgDimensionType.org.getId(), orgIdList);
    }


    /**
     * 绑定用户到机构(部门)
     *
     * @param userIdList    用户ID
     * @param orgIdList     机构Id
     * @param removeOldBind 是否删除旧的绑定信息
     * @return void
     * @see DimensionUserBindUtils#bindUser(DefaultDimensionUserService, Collection, String, Collection, boolean)
     */
    @Transactional
    public Mono<Void> bindUser(Collection<String> userIdList,
                               Collection<String> orgIdList,
                               boolean removeOldBind) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Mono.empty();
        }
        return DimensionUserBindUtils.bindUser(dimensionUserService, userIdList, OrgDimensionType.org.getId(), orgIdList, removeOldBind);
    }


    public List<OrganizationEntity> getCachedTree(Collection<String> idList) {
        return TreeUtils.list2tree(
            Collections2.transform(inMemoryCache.values(), e -> e.copyTo(new OrganizationEntity())),
            OrganizationEntity::getId,
            OrganizationEntity::getParentId,
            OrganizationEntity::setChildren,
            (helper, e) -> idList.contains(e.getId()));
    }

    public Optional<OrganizationEntity> getCached(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(inMemoryCache.get(id));
    }

    @EventListener
    public void doHandleCreated(EntityCreatedEvent<OrganizationEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .doOnNext(entity -> inMemoryCache.put(entity.getId(), entity))
        );
    }

    @EventListener
    public void doHandleSaved(EntitySavedEvent<OrganizationEntity> event) {

        event.async(
            Flux.fromIterable(event.getEntity())
                .doOnNext(entity -> inMemoryCache.put(entity.getId(), entity))
                .doOnNext(this::handleParentId)
        );
    }

    @EventListener
    public void doHandleDeleted(EntityDeletedEvent<OrganizationEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .doOnNext(entity -> inMemoryCache.remove(entity.getId()))
        );
    }

    @EventListener
    public void doHandleModified(EntityModifyEvent<OrganizationEntity> event) {
        event.async(
            Flux.fromIterable(event.getAfter())
                .doOnNext(entity -> inMemoryCache.put(entity.getId(), entity))
                .doOnNext(this::handleParentId)
        );
    }

    @Override
    public void run(String... args) throws Exception {
        createQuery()
            .fetch()
            .subscribe(organization -> {
                inMemoryCache.put(organization.getId(), organization);
            });
    }

    private Mono<Void> handleParentId(OrganizationEntity organization){
        return Mono
            .just(organization)
            .filter(org -> StringUtils.isBlank(organization.getParentId()))
            .flatMap(org -> createUpdate()
                .setNull(OrganizationEntity::getParentId)
                .where(OrganizationEntity::getId, organization.getId())
                .execute()
                .as(EntityEventHelper::setDoNotFireEvent)
                .then()
            );
    }

    /**
     * 获取所有组织机构（树形）
     *
     * @param query   查询条件
     * @param isCount 是否统计各层级组织机构人数 默认false 表示不统计
     * @return Flux<OrganizationDetail>
     */
    public Flux<OrganizationDetail> getAllOrgTreeForMember(QueryParamEntity query, boolean isCount) {
        if (isCount) {
            Set<String> orgIds = new HashSet<>();
            return this.query(query)
                       .map(OrganizationDetail::from)
                       .doOnNext(detail -> orgIds.add(detail.getId()))
                       .collectList()
                       .map(list ->
                                TreeUtils.list2tree(list,
                                                    OrganizationDetail::getId,
                                                    OrganizationDetail::getParentId,
                                                    OrganizationDetail::setChildren))
                       .flatMapMany(item ->
                                        dimensionUserService.createQuery()
                                                            .select(DimensionUserEntity::getDimensionId, DimensionUserEntity::getUserId)
                                                            .where(DimensionUserEntity::getDimensionTypeId, OrgDimensionType.org.getId())
                                                            .in(DimensionUserEntity::getDimensionId, orgIds)
                                                            .fetch()
                                                            .doOnNext(dimUsers -> orgIds.clear())
                                                            .collectMultimap(DimensionUserEntity::getDimensionId, DimensionUserEntity::getUserId)
                                                            .flatMapMany(orgUserIdMap ->
                                                                             Flux.fromIterable(item)
                                                                                 .doOnNext(detail -> this.countOrgMemberSize(detail, orgUserIdMap))));
        }
        return this.queryResultToTree(query)
                   .flatMapMany(Flux::fromIterable)
                   .map(OrganizationDetail::from);
    }

    /**
     * 统计各层级组织下人数
     *
     * @param entity       每层级组织机构信息
     * @param orgUserIdMap 组织和用户的id映射map
     */
    private long countOrgMemberSize(OrganizationDetail entity, Map<String, Collection<String>> orgUserIdMap) {
        if (CollectionUtils.isNotEmpty(entity.getChildren())) {
            for (OrganizationDetail child : entity.getChildren()) {
                long subCount = this.countOrgMemberSize(child, orgUserIdMap);
                entity.addMemberCount(subCount);
            }
        }
        // 子级用户重复统计的次数
        long duplicationCount = 0L;
        Collection<String> userIds = orgUserIdMap.remove(entity.getId());
        if (CollectionUtils.isNotEmpty(userIds)) {
            entity.fillMemberCount(userIds.size());
            Collection<String> parentUserIds = orgUserIdMap.get(entity.getParentId());
            if (CollectionUtils.isNotEmpty(parentUserIds)) {
                duplicationCount = parentUserIds.stream()
                                                .filter(userIds::contains)
                                                .count();
            }
        }
        return entity.getMemberCount() - duplicationCount;
    }

}
