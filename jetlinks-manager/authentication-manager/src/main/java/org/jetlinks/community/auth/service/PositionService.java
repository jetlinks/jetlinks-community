package org.jetlinks.community.auth.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Collections2;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.TreeUtils;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.PositionDetail;
import org.jetlinks.community.auth.entity.PositionEntity;
import org.jetlinks.community.auth.entity.PositionRoleEntity;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.service.info.OrgPositionUnbindInfo;
import org.jetlinks.community.auth.service.info.PositionRoleBindInfo;
import org.jetlinks.community.auth.utils.DimensionUserBindUtils;
import org.jetlinks.community.authorize.OrgDimensionType;
//import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 职位服务
 *
 * @author zhouhao
 * @see org.jetlinks.community.auth.event.handler.PositionEventHandler
 * @since 2.3
 */
@Service
@AllArgsConstructor
public class PositionService extends GenericReactiveTreeSupportCrudService<PositionEntity, String> implements CommandLineRunner {

    private static final String DIMENSION_ORG_RELATION = OrgDimensionType.position.getId();

    private final DefaultDimensionUserService dimensionUserService;

    @SuppressWarnings("all")
    private final ReactiveRepository<PositionRoleEntity, String> roleBindRepository;

    private final QueryHelper helper;

    private final OrganizationService organizationService;

    private final Map<String, PositionEntity> inMemoryCache = new ConcurrentHashMap<>();

    private final Map<Set<String>, Flux<PositionRoleBindInfo>> positionRolesLoaderCache =
        Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(1))
                .<Set<String>, Flux<PositionRoleBindInfo>>build()
                .asMap();

    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.RANDOM;
    }

    public void clearCache() {
        positionRolesLoaderCache.clear();
    }

    @Override
    public void setChildren(PositionEntity entity, List<PositionEntity> children) {
        entity.setChildren(children);
    }

    private Flux<PositionRoleBindInfo> getPositionRoles0(Collection<String> idList) {
        return helper
            .select(PositionRoleBindInfo.class)
            .as(PositionRoleEntity::getPositionId, PositionRoleBindInfo::setPositionId)
            .all(RoleEntity.class, PositionRoleBindInfo::setRole)
            .from(RoleEntity.class)
            .innerJoin(PositionRoleEntity.class,
                       on -> on
                           .is(RoleEntity::getId, PositionRoleEntity::getRoleId))
            .where(c -> c.in(PositionRoleEntity::getPositionId, idList))
            .fetch();
    }

    public Flux<PositionRoleBindInfo> getPositionRoles(Collection<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Flux.empty();
        }
        //使用缓存避免大量用户初始化时造成压力过大
        return positionRolesLoaderCache
            .computeIfAbsent(
                new HashSet<>(idList), _idList -> getPositionRoles0(_idList).cache(Duration.ofSeconds(1))
            );
    }

    public Mono<Map<String, List<RoleEntity>>> getPositionRoleMapping(Collection<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Mono.just(Collections.emptyMap());
        }
        return getPositionRoles(idList)
            .collect(Collectors.groupingBy(
                PositionRoleBindInfo::getPositionId,
                Collectors.mapping(PositionRoleBindInfo::getRole, Collectors.toList())));
    }

    public Mono<Map<String, String>> getParentPositionMapping(Collection<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Mono.just(Collections.emptyMap());
        }
        return this
            .createQuery()
            .select(PositionEntity::getId)
            .select(PositionEntity::getName)
            .in(PositionEntity::getId, idList)
            .fetch()
            .collect(Collectors.toMap(PositionEntity::getId, PositionEntity::getName));
    }

    public Mono<Map<String, Long>> getPositionMemberCountMapping(Collection<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return Mono.just(Collections.emptyMap());
        }
        return dimensionUserService
            .createQuery()
            .where()
            .in(DimensionUserEntity::getDimensionId, idList)
            .fetch()
            .collect(Collectors.groupingBy(DimensionUserEntity::getDimensionId, Collectors.counting()));
    }




    public Flux<PositionDetail> convertPositionDetail(Collection<PositionEntity> entities) {
        Collection<String> posId = Collections2.transform(entities, PositionEntity::getId);
        Collection<String> posParentId = Collections2.transform(entities, PositionEntity::getParentId);
        return Mono
            .zip(
                this.getPositionRoleMapping(posId),
                this.getParentPositionMapping(posParentId),
                this.getPositionMemberCountMapping(posId)
            )
            .flatMapMany(tp3 -> {
                Map<String, List<RoleEntity>> positionRoleMapping = tp3.getT1();
                Map<String, String> parentPositionMapping = tp3.getT2();
                Map<String, Long> positionMemberCountMapping = tp3.getT3();
                return Flux
                    .fromIterable(entities)
                    .map(e -> {
                        // 角色信息
                        PositionDetail detail = PositionDetail.from(e);
                        detail.withRoles(positionRoleMapping.get(e.getId()));
                        // 父级职位名称
                        if (StringUtils.hasText(detail.getParentId())){
                           detail.setParentName(parentPositionMapping.get(detail.getParentId()));
                        }
                        // 职位成员数量
                        Long count = positionMemberCountMapping.get(detail.getId());
                        detail.setMemberCount(count != null ? count.intValue() : 0);

                        // 组织名称
                        organizationService.getCached(e.getOrgId()).ifPresent(org -> detail.setOrgName(org.getName()));
                        return detail;
                    });
            });
    }

    // 解除组织绑定时，解除用户当前组织所对应的所有职位绑定
    @Subscribe(value = "/unbind/user/position")
    public Mono<Void> syncUnbindPosition(OrgPositionUnbindInfo info) {
        return this
            .unbindUserByPosition(info.getUserIds(), info.getOrgIds())
            .then();
    }

    private Mono<Integer> unbindUserByPosition(@NotNull List<String> userIdList,
                                               @Nullable List<String> orgIdList) {
        return createQuery()
            .in(PositionEntity::getOrgId, orgIdList)
            .fetch()
            .map(PositionEntity::getId)
            .collectList()
            .flatMap(positionIds -> DimensionUserBindUtils
                .unbindUser(dimensionUserService, userIdList, OrgDimensionType.position.getId(), positionIds));
    }

    public Flux<PositionDetail> getPositionDetail(Collection<String> idList, boolean cached) {
        return this
            .getPositionRoleMapping(idList)
            .flatMapMany(mappings -> {
                Flux<PositionEntity> positionFlux;
                if (cached) {
                    positionFlux = Flux
                        .fromIterable(idList)
                        .mapNotNull(inMemoryCache::get);
                } else {
                    positionFlux = createQuery()
                        .in(PositionEntity::getId, idList)
                        .fetch();
                }
                return positionFlux
                    .map(e -> {
                        PositionDetail detail = PositionDetail.from(e);
                        detail.withRoles(mappings.get(e.getId()));
                        return detail;
                    });
            });
    }

    /**
     * 保存职位详情信息,包括职位信息,职位绑定的角色信息.
     *
     * @param detail 职位详情信息
     * @return void
     */
    @Transactional
    public Mono<Void> saveDetail(Flux<PositionDetail> detail) {
        Flux<PositionDetail> cache = detail.cache();

        return cache
            .mapNotNull(PositionDetail::getId)
            .collectList()
            .filter(CollectionUtils::isNotEmpty)
            //删除旧职位信息
            .flatMap(list -> roleBindRepository
                .createDelete()
                .where()
                .in(PositionRoleEntity::getPositionId, list)
                .execute()
                //不触发事件,由PositionEntity事件统一处理.
                .as(EntityEventHelper::setDoNotFireEvent))
            //保存职位信息
            .then(cache.map(PositionDetail::toEntity).as(this::save))
            //保存绑定信息
            .then(cache
                      .flatMapIterable(e -> Collections2
                          .transform(
                              e.getRoleIdList(),
                              roleId -> {
                                  PositionRoleEntity entity = new PositionRoleEntity();
                                  entity.setPositionId(e.getId());
                                  entity.setRoleId(roleId);
                                  return entity;
                              }
                          ))
                      .as(roleBindRepository::save)
                      //不触发事件,由PositionEntity事件统一处理.
                      .as(EntityEventHelper::setDoNotFireEvent)
            )
            .then();
    }

    /**
     * 保存职位详情信息,包括职位信息,职位绑定的角色信息.
     *
     * @param detailMono 职位详情信息
     * @return void
     */
    @Transactional
    public Mono<PositionEntity> saveDetail(Mono<PositionDetail> detailMono) {

        return detailMono
            .flatMap(detail -> {
                PositionEntity position = detail.toEntity();

                return this
                    .insert(position)
                    .flatMapMany(ignore -> Flux
                        .fromIterable(Collections2.transform(
                            detail.getRoleIdList(),
                            roleId -> {
                                PositionRoleEntity entity = new PositionRoleEntity();
                                entity.setPositionId(position.getId());
                                entity.setRoleId(roleId);
                                return entity;
                            }
                        ))
                        .as(roleBindRepository::save)
                        //不触发事件,由PositionEntity事件统一处理.
                        .as(EntityEventHelper::setDoNotFireEvent)
                    )
                    .then()
                    .thenReturn(position);
            });
    }


    @Transactional
    public Mono<Integer> bindRole(String id, Collection<String> roleIdList) {
        return Flux
            .fromIterable(roleIdList)
            .map(roleId -> {
                PositionRoleEntity entity = new PositionRoleEntity();
                entity.setPositionId(id);
                entity.setRoleId(roleId);
                return entity;
            })
            .as(roleBindRepository::save)
            .map(SaveResult::getTotal);
    }

    @Transactional
    public Mono<Integer> unbindRole(String id, Collection<String> roleIdList) {
        return roleBindRepository
            .deleteById(Collections2.transform(
                roleIdList,
                roleId -> PositionRoleEntity.generateId(id, roleId)));
    }

    @Transactional
    public Mono<Integer> unbindUser(@NotNull List<String> userIdList,
                                    @Nullable List<String> idList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Mono.empty();
        }

        return DimensionUserBindUtils
            .unbindUser(dimensionUserService, userIdList, OrgDimensionType.position.getId(), idList);
    }


    /**
     * 绑定用户到职位
     *
     * @param userIdList     用户ID
     * @param positionIdList 职位Id
     * @param removeOldBind  是否删除旧的绑定信息
     * @return void
     * @see DimensionUserBindUtils#bindUser(DefaultDimensionUserService, Collection, String, Collection, boolean)
     */
    @Transactional
    public Mono<Void> bindUser(Collection<String> userIdList,
                               Collection<String> positionIdList,
                               boolean removeOldBind) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return Mono.empty();
        }
//        Set<String> orgIdList = new HashSet<>();
//        for (String s : positionIdList) {
//            PositionEntity entity = inMemoryCache.get(s);
//            if (entity != null) {
//                orgIdList.add(entity.getOrgId());
//            }
//        }

        return
            //不自动绑定组织,由PositionDimensionProvider自动绑定
//            DimensionUserBindUtils
//            .bindUser(dimensionUserService,
//                      userIdList,
//                      OrgDimensionType.org.getId(),
//                      orgIdList,
//                      removeOldBind,
//                      del -> del.is(DimensionUserEntity::getRelation, DIMENSION_ORG_RELATION),
//                      entity -> {
//                          entity.setRelation(DIMENSION_ORG_RELATION);
//                          return entity;
//                      })
//            .then(
            DimensionUserBindUtils
                .bindUser(dimensionUserService,
                          userIdList,
                          OrgDimensionType.position.getId(),
                          positionIdList,
                          removeOldBind)
            //    )
            ;
    }


    public List<PositionEntity> getCachedTree(Collection<String> idList) {
        return TreeUtils.list2tree(
            Collections2.transform(inMemoryCache.values(), e -> e.copyTo(new PositionEntity())),
            PositionEntity::getId,
            PositionEntity::getParentId,
            PositionEntity::setChildren,
            (helper, e) -> idList.contains(e.getId()));
    }


    public Optional<PositionEntity> getCached(String id) {
        return Optional.ofNullable(inMemoryCache.get(id));
    }

    @EventListener
    public void doHandleCreated(EntityCreatedEvent<PositionEntity> event) {
        event.async(
                Flux.fromIterable(event.getEntity())
                    .doOnNext(entity -> inMemoryCache.put(entity.getId(), entity))
        );
    }

    @EventListener
    public void doHandleSaved(EntitySavedEvent<PositionEntity> event) {
        event.async(
                Flux.fromIterable(event.getEntity())
                    .doOnNext(entity -> inMemoryCache.put(entity.getId(), entity))
        );
    }

    @EventListener
    public void doHandleDeleted(EntityDeletedEvent<PositionEntity> event) {
        event.async(
                Flux.fromIterable(event.getEntity())
                    .doOnNext(entity -> inMemoryCache.remove(entity.getId()))
        );
    }

    @EventListener
    public void doHandleModified(EntityModifyEvent<PositionEntity> event) {
        event.async(
                Flux.fromIterable(event.getAfter())
                    .doOnNext(entity -> inMemoryCache.put(entity.getId(), entity))
        );
    }


    @Override
    public void run(String... args) throws Exception {
        createQuery()
            .fetch()
            .subscribe(entity -> {
                inMemoryCache.put(entity.getId(), entity);
            });
    }

    /**
     * 清除上级职位信息
     * @param event
     */
    @EventListener
    public void handleSaved(EntitySavedEvent<PositionEntity> event){
        event.async(
            handleUpdate(Flux.fromIterable(event.getEntity()))
        );
    }

    /**
     * 清除上级职位信息
     * @param event
     */
    @EventListener
    public void handleModify(EntityModifyEvent<PositionEntity> event){
        event.async(
            handleUpdate(Flux.fromIterable(event.getAfter()))
        );
    }

    private Mono<Integer> handleUpdate(Flux<PositionEntity> flux) {
        return flux
            .filter(pos -> !StringUtils.hasText(pos.getParentId()))
            .map(PositionEntity::getId)
            .collectList()
            .flatMap(posIds -> {
                if (CollectionUtils.isNotEmpty(posIds)) {
                    return this
                        .createUpdate()
                        .setNull(PositionEntity::getParentId)
                        .where()
                        .in(PositionEntity::getId, posIds)
                        .execute();
                }
                return Mono.empty();
            })
            .as(EntityEventHelper::setDoNotFireEvent);
    }
}
