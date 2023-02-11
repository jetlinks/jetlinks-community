package org.jetlinks.community.auth.dimension;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionType;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.hswebframework.web.system.authorization.defaults.service.terms.DimensionTerm;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OrganizationDimensionProvider extends BaseDimensionProvider<OrganizationEntity> {

    public OrganizationDimensionProvider(ReactiveRepository<OrganizationEntity, String> repository,
                                         DefaultDimensionUserService dimensionUserService,
                                         ApplicationEventPublisher eventPublisher) {
        super(repository, eventPublisher, dimensionUserService);
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

}
