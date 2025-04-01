package org.jetlinks.community.auth.service;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.token.UserToken;
import org.hswebframework.web.authorization.token.UserTokenManager;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.hswebframework.web.system.authorization.api.event.UserDeletedEvent;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.auth.entity.*;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.auth.enums.UserEntityTypes;
import org.jetlinks.community.auth.service.info.UserLoginConstant;
import org.jetlinks.community.auth.service.info.UserLoginInfo;
import org.jetlinks.community.auth.service.request.SaveUserDetailRequest;
import org.jetlinks.community.auth.service.request.SaveUserRequest;
import org.jetlinks.community.commons.EmbeddedThingTypes;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.core.things.ThingsRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

/**
 * 用户详情管理
 * <p>
 * 通过通用增删改查接口实现用户详情增删改查功能.
 * 通过用户id获取用户基本信息（包含租户成员信息）
 *
 * @author zhouhao
 * @see org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService
 * @since 1.3
 */
@Service
public class UserDetailService extends GenericReactiveCrudService<UserDetailEntity, String> {

    public static final String recursiveTagContextKey = UserDetailService.class.getName() + "_RECURSIVE_TAG";

    private final ReactiveUserService userService;

    private final RoleService roleService;
    private final OrganizationService organizationService;
    private final PositionService positionService;
    private final static UserDetailEntity emptyDetail = new UserDetailEntity();

    private final ApplicationEventPublisher eventPublisher;
    private final UserTokenManager userTokenManager;
    private final UserSettingService userSettingService;
    private final QueryHelper queryHelper;

    private final ThingsRegistry registry;

    public UserDetailService(ReactiveUserService userService,
                             RoleService roleService,
                             OrganizationService organizationService,
                             PositionService positionService,
                             ApplicationEventPublisher eventPublisher,
                             UserTokenManager userTokenManager,
                             UserSettingService userSettingService,
                             QueryHelper queryHelper,
                             ThingsRegistry registry) {
        this.userService = userService;
        this.roleService = roleService;
        this.positionService = positionService;
        this.organizationService = organizationService;
        this.eventPublisher = eventPublisher;
        this.userTokenManager = userTokenManager;
        this.userSettingService = userSettingService;
        this.queryHelper = queryHelper;
        this.registry = registry;
        // 注册默认用户类型
        UserEntityTypes.register(Arrays.asList(DefaultUserEntityType.values()));
    }

    /**
     * 根据用户id获取用户详情
     *
     * @param userId 用户id
     * @return 详情信息
     */
    @Transactional(readOnly = true)
    public Mono<UserDetail> findUserDetail(String userId) {
        return Mono
            .zip(
                userService.findById(userId), // 基本信息
                this.findById(userId).defaultIfEmpty(emptyDetail), // 详情
                (user, detail) -> fillUserDetail(UserDetail.of(user).with(detail))
            )
            .flatMap(Function.identity());
    }

    /**
     * 根据用户id和用户信息保存用户详情
     *
     * @param userId  用户ID
     * @param request 详情信息
     * @return void
     */
    @Transactional(rollbackFor = Throwable.class)
    public Mono<Void> saveUserDetail(String userId, SaveUserDetailRequest request) {
        ValidatorUtils.tryValidate(request);
        UserDetailEntity entity = FastBeanCopier.copy(request, new UserDetailEntity());
        entity.setId(userId);

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);
        userEntity.setName(request.getName());

        return this
            .save(entity)
            .then(userService.saveUser(Mono.just(userEntity)))
            .as(LocaleUtils::transform)
            .then();
    }

    public Mono<PagerResult<UserDetail>> queryUserDetail(QueryParamEntity query) {
        return QueryHelper
            .transformPageResult(
                queryHelper
                    .select(UserDetail.class)
                    .all(UserDetailEntity.class)
                    .as(UserEntity::getId, UserDetail::setId)
                    .as(UserEntity::getName, UserDetail::setName)
                    .as(UserEntity::getUsername, UserDetail::setUsername)
                    //兼容之前已有字段
                    .as(UserEntity::getType, UserDetail::setTypeId)
                    .as(UserEntity::getStatus, UserDetail::setStatus)
                    .as(UserEntity::getCreateTime, UserDetail::setCreateTime)
                    .as(UserEntity::getCreatorId, UserDetail::setCreatorId)
                    .from(UserEntity.class)
                    .leftJoin(UserDetailEntity.class, j -> j.is(UserDetailEntity::getId, UserEntity::getId))
                    .where(query)
                    .fetchPaged(),
                list -> this
                    .fillUserDetail(list)
                    .collectList()
            );
    }

    private Mono<UserDetail> fillUserDetail(UserDetail detail) {
        return Mono
            .zip(
                //用户维度信息
                ReactiveAuthenticationHolder
                    .get(detail.getId())
                    .map(Authentication::getDimensions)
                    .defaultIfEmpty(Collections.emptyList()),
                this
                    .getLastRequestTime(detail.getId())
                    .defaultIfEmpty(0L),
                this
                    .getLoginInfo(detail.getId())
                    .defaultIfEmpty(UserLoginInfo.EMPTY)
            )
            .map(tps -> detail
                .withDimension(tps.getT1())
                .withLastRequestTime(tps.getT2())
                .withUserLoginInfo(tps.getT3()))
            //填充用户组织机构完整名称
            .flatMap(this::refactorUserDetail);

    }

    public Flux<UserDetail> fillUserDetail(List<UserDetail> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Flux.empty();
        }
        return Flux
            .fromIterable(users)
            .flatMap(this::fillUserDetail, 8)
            .thenMany(Flux.fromIterable(users));
    }

    /**
     * 保存用户,自动关联角色{@link SaveUserRequest#getRoleIdList()}以及机构(部门){@link SaveUserRequest#getOrgIdList()}
     *
     * @param request 保存请求
     * @return 用户ID
     */
    @Transactional
    public Mono<String> saveUser(SaveUserRequest request) {
        request.validate();
        UserDetail detail = request.getUser();
        boolean isUpdate = StringUtils.hasText(detail.getId());
        UserEntity entity = request.getUser().toUserEntity();
        return userService
            .saveUser(Mono.just(entity))
            .then(Mono.fromSupplier(entity::getId))
            .flatMap(userId -> {
                detail.setId(userId);
                //保存详情
                return this
                    .save(detail.toDetailEntity())
                    //绑定角色
                    .then(roleService.bindUser(Collections.singleton(userId), request.getRoleIdList(), isUpdate))
                    //绑定机构部门
                    .then(organizationService.bindUser(Collections.singleton(userId), request.getOrgIdList(), isUpdate))
                    //绑定岗位
                    .then(positionService.bindUser(Collections.singleton(userId), request.getPositions(), isUpdate))
                    .thenReturn(userId);
            })
            //禁用上游产生的清空用户权限事件,因为可能会导致重复执行
            .as(ClearUserAuthorizationCacheEvent::disable)
            //只执行一次清空用户权限事件
            .flatMap(userId -> ClearUserAuthorizationCacheEvent.of(userId).publish(eventPublisher).thenReturn(userId))
            .as(LocaleUtils::transform);
    }

    /**
     * 删除用户时同时删除用户详情
     *
     * @param event 用户删除事件
     */
    @EventListener
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        event.async(
            this.deleteById(event.getUser().getId())
        );
    }

    private Mono<Long> getLastRequestTime(String userId) {
        return userTokenManager
            .getByUserId(userId)
            .filter(UserToken::isNormal)
            .map(UserToken::getLastRequestTime)
            .reduce(Math::max);
    }

    private Mono<UserLoginInfo> getLoginInfo(String userId) {
        return registry
            .getThing(EmbeddedThingTypes.user, userId)
            .flatMap(UserLoginInfo::readFrom)
            .switchIfEmpty(Mono.defer(() -> {
                // todo 弃用此方式
                return userSettingService
                    .findById(UserSettingEntity.generateId(userId, UserLoginConstant.USER_LOGIN_INFO, UserLoginConstant.LAST_LOGIN_INFO))
                    .map(s -> ObjectMappers.parseJson(s.getContent(), UserLoginInfo.class));
            }));

    }

    private void refactorOrg(OrganizationInfo info) {
        OrganizationEntity org = organizationService.getCached(info.getId()).orElse(null);
        if (org == null) {
            return;
        }
        info.setName(org.getName());
        String parentId = org.getParentId();

        while (StringUtils.hasText(parentId)) {
            org = organizationService.getCached(parentId).orElse(null);
            if (org == null) {
                break;
            }
            parentId = org.getParentId();
            info.addParentFullName(org.getName());
        }
    }

    private OrganizationInfo refactorPosition(PositionDetail position) {
        OrganizationEntity org = organizationService
            .getCached(position.getOrgId())
            .orElse(null);
        if (org == null) {
            return null;
        }
        OrganizationInfo info = OrganizationInfo.from(org);
        refactorOrg(info);
        position.withOrg(info);
        return info;
    }

    /**
     * 填充用户组织、职位等完整信息
     *
     * @param userDetail 用户信息
     * @return 用户所属组织的完整名称集合
     */
    private Mono<UserDetail> refactorUserDetail(UserDetail userDetail) {

        Map<String, OrganizationInfo> orgMap = new HashMap<>();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(userDetail.getOrgList())) {
            for (OrganizationInfo organizationInfo : userDetail.getOrgList()) {
                refactorOrg(organizationInfo);
                orgMap.put(organizationInfo.getId(), organizationInfo);
            }
        }

        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(userDetail.getPositions())) {
            for (PositionDetail position : userDetail.getPositions()) {
                OrganizationInfo org = refactorPosition(position);
                if (org != null) {
                    orgMap.put(org.getId(), org);
                }
            }
        }
        //可能职位的组织不是用户的所在组织, 可能是下级组织的职位.
        if (!orgMap.isEmpty()) {
            List<OrganizationInfo> newOrg = new ArrayList<>(orgMap.values());
            newOrg.sort(Comparator.comparing(OrganizationInfo::getSortIndex));
            userDetail.setOrgList(newOrg);
        }

        return Mono.just(userDetail);
//        return Flux.fromIterable(userDetail.getOrgList())
//                   .collect(HashSet<String>::new, (set, value) -> set.add(value.getId()))
//                   .flatMapMany(orgIds ->
//                                    organizationService.findById(orgIds)
//                                                       .map(OrganizationInfo::from)
//                                                       .collectList()
//                                                       .doOnNext(userDetail::setOrgList)
//                                                       .flatMapMany(Flux::fromIterable)
//                                                       .filter(info -> StringUtils.hasText(info.getParentId()))
//                                                       .collect(HashSet<String>::new, (set, info) -> set.add(info.getParentId()))
//                                                       .flatMapMany(parentIds ->
//                                                                        organizationService.createQuery()
//                                                                                           .in(OrganizationEntity::getId, parentIds)
//                                                                                           .fetch())
//                                                       .collectMap(OrganizationEntity::getId, OrganizationEntity::getName)
//                                                       .flatMapMany(map ->
//                                                                        Flux.fromIterable(userDetail.getOrgList())
//                                                                            .doOnNext(organizationInfo ->
//                                                                                          organizationInfo.addParentFullName(map.get(organizationInfo.getParentId()))))
//                   )
//                   .then(Mono.just(userDetail));
    }

    /**
     * 查询用户信息（包含用户维度关联信息）
     *
     * @param query         查询条件
     * @param dimensionType 维度类型
     * @param dimensionId   维度id
     * @return Mono<PagerResult < UserDetail>>
     */
    public Mono<PagerResult<UserDetail>> queryUserIncludeDimensionInfo(QueryParamEntity query,
                                                                       String dimensionType,
                                                                       String dimensionId) {
        return queryHelper
            .select(UserDetail.class)
            .as(UserEntity::getId, UserDetail::setId)
            .as(UserEntity::getName, UserDetail::setName)
            .as(UserEntity::getUsername, UserDetail::setUsername)
            .as(UserEntity::getType, UserDetail::setTypeId)
            .as(UserEntity::getStatus, UserDetail::setStatus)
            .as(UserEntity::getCreateTime, UserDetail::setCreateTime)
            .as(UserEntity::getCreatorId, UserDetail::setCreatorId)
            .as(DimensionUserEntity::getRelationTime, UserDetail::setRelationTime)
            .from(UserEntity.class)
            .leftJoin(DimensionUserEntity.class, j ->
                j.is(DimensionUserEntity::getUserId, UserEntity::getId)
                 .and(DimensionUserEntity::getDimensionTypeId, dimensionType)
                 .and(DimensionUserEntity::getDimensionId, dimensionId))
            .where(query)
            .fetchPaged();
    }

}
