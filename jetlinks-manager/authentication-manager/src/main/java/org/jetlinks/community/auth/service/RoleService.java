package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.utils.DimensionUserBindUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Collection;

@Service
@AllArgsConstructor
public class RoleService extends GenericReactiveCacheSupportCrudService<RoleEntity, String> {


    private final DefaultDimensionUserService dimensionUserService;

    private QueryHelper queryHelper;


    /**
     * 绑定用户到角色
     *
     * @param userIdList    用户ID
     * @param roleIdList    角色Id
     * @param removeOldBind 是否删除旧的绑定信息
     * @return void
     * @see DimensionUserBindUtils#bindUser(DefaultDimensionUserService, Collection, String, Collection, boolean)
     */
    @Transactional
    public Mono<Void> bindUser(@NotNull Collection<String> userIdList,
                               @Nullable Collection<String> roleIdList,
                               boolean removeOldBind) {

        if (CollectionUtils.isEmpty(userIdList)) {
            return Mono.empty();
        }

        return DimensionUserBindUtils
            .bindUser(dimensionUserService,
                      userIdList,
                      DefaultDimensionType.role.getId(),
                      roleIdList,
                      removeOldBind);

    }

    /**
     * 解绑角色的用户
     *
     * @param userIdList 用户ID
     * @param roleIdList 角色Id
     * @return void
     * @see DimensionUserBindUtils#unbindUser(DefaultDimensionUserService, Collection, String, Collection)
     */
    @Transactional
    public Mono<Void> unbindUser(@NotNull Collection<String> userIdList,
                                 @Nullable Collection<String> roleIdList) {

        if (CollectionUtils.isEmpty(userIdList)) {
            return Mono.empty();
        }
        return DimensionUserBindUtils
            .unbindUser(dimensionUserService, userIdList, DefaultDimensionType.role.getId(), roleIdList)
            .then();

    }

    /**
     * 解绑角色的所有用户
     *
     * @param roleIdList 角色Id
     * @return void
     * @see DimensionUserBindUtils#unbindUser(DefaultDimensionUserService, Collection, String, Collection)
     */
    @Transactional
    public Mono<Void> unbindAllUser(@NotNull Collection<String> roleIdList) {

        if (CollectionUtils.isEmpty(roleIdList)) {
            return Mono.empty();
        }
        return DimensionUserBindUtils
            .unbindUser(dimensionUserService, null, DefaultDimensionType.role.getId(), roleIdList)
            .then();

    }
}
