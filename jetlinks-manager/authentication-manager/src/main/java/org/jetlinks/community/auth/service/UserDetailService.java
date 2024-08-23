package org.jetlinks.community.auth.service;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.ReactiveAuthenticationHolder;
import org.hswebframework.web.authorization.ReactiveAuthenticationManager;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.hswebframework.web.system.authorization.api.event.UserDeletedEvent;
import org.hswebframework.web.system.authorization.api.service.reactive.ReactiveUserService;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.entity.UserDetailEntity;
import org.jetlinks.community.auth.enums.DefaultUserEntityType;
import org.jetlinks.community.auth.enums.UserEntityTypes;
import org.jetlinks.community.auth.service.request.SaveUserDetailRequest;
import org.jetlinks.community.auth.service.request.SaveUserRequest;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    private final ReactiveUserService userService;

    private final RoleService roleService;

    private final OrganizationService organizationService;

    private final ReactiveAuthenticationManager authenticationManager;

    private final QueryHelper queryHelper;

    private final static UserDetailEntity emptyDetail = new UserDetailEntity();

    public UserDetailService(ReactiveUserService userService,
                             RoleService roleService,
                             OrganizationService organizationService,
                             ReactiveAuthenticationManager authenticationManager,
                             QueryHelper queryHelper) {
        this.userService = userService;
        this.roleService = roleService;
        this.organizationService = organizationService;
        this.authenticationManager = authenticationManager;
        this.queryHelper = queryHelper;
        // 注册默认用户类型
        UserEntityTypes.register(Arrays.asList(DefaultUserEntityType.values()));

    }

    /**
     * 根据用户id获取用户详情
     *
     * @param userId 用户id
     * @return 详情信息
     */
    public Mono<UserDetail> findUserDetail(String userId) {
        return Mono
            .zip(
                userService.findById(userId), // 基本信息
                this.findById(userId).defaultIfEmpty(emptyDetail), // 详情
//                memberService.findMemberDetail(userId).collectList(), // 租户成员信息
                authenticationManager       //用户维度信息
                                            .getByUserId(userId)
                                            .map(Authentication::getDimensions)
                                            .defaultIfEmpty(Collections.emptyList())
            )
            .map(tp4 -> UserDetail
                     .of(tp4.getT1())
                     .with(tp4.getT2())
//                .with(tp4.getT3())
                     .withDimension(tp4.getT3())
            );
    }

    /**
     * 根据用户id和用户信息保存用户详情
     *
     * @param userId  用户ID
     * @param request 详情信息
     * @return void
     */
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
                    // 兼容之前已有字段
                    .as(UserEntity::getType, UserDetail::setTypeId)
                    .as(UserEntity::getStatus, UserDetail::setStatus)
                    .as(UserEntity::getCreateTime, UserDetail::setCreateTime)
                    .as(UserEntity::getCreatorId, UserDetail::setCreatorId)
                    .from(UserEntity.class)
                    .leftJoin(UserDetailEntity.class, j -> j.is(UserDetailEntity::getId, UserEntity::getId))
                    .where(query)
                    .fetchPaged(),
                list -> this.fillUserDetail(list).
                            collectList());
    }

    private Flux<UserDetail> fillUserDetail(List<UserDetail> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Flux.empty();
        }
        return Flux
            .fromIterable(users)
            .flatMap(detail ->
                         //维度信息
                         ReactiveAuthenticationHolder
                             .get(detail.getId())
                             .map(Authentication::getDimensions)
                             .defaultIfEmpty(Collections.emptyList())
                             .map(dimensions -> detail.withDimension(dimensions).withType())
            );
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
                    .thenReturn(userId);
            })
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

}
