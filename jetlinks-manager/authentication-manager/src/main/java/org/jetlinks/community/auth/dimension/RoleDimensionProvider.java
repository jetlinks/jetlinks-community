package org.jetlinks.community.auth.dimension;

import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.enums.RoleState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RoleDimensionProvider extends BaseDimensionProvider<RoleEntity> {

    public RoleDimensionProvider(ReactiveRepository<RoleEntity, String> repository,
                                 DefaultDimensionUserService dimensionUserService,
                                 ApplicationEventPublisher eventPublisher) {
        super(repository, eventPublisher, dimensionUserService);
    }

    @Override
    protected DimensionType getDimensionType() {
        return DefaultDimensionType.role;
    }

    @Override
    protected Mono<Dimension> convertToDimension(RoleEntity entity) {

        return Mono.just(entity.toDimension());
    }

    @Override
    protected ReactiveQuery<RoleEntity> createQuery() {
        return super
            .createQuery()
            .and(RoleEntity::getState, RoleState.enabled.getValue());
    }

}
