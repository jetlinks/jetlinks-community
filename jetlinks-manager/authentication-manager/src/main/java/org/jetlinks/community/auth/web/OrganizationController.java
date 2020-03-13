package org.jetlinks.community.auth.web;

import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.system.authorization.api.entity.DimensionEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/organization")
@RestController
@Resource(id = "organization", name = "机构管理")
public class OrganizationController {

    static String orgDimensionTypeId = "org";

    @Autowired
    private DefaultDimensionService dimensionService;

    @GetMapping("/_all/tree")
    @Authorize(merge = false)
    public Flux<DimensionEntity> getAllOrgTree() {
        return getAllOrg()
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity.list2tree(list, DimensionEntity::setChildren));
    }

    @GetMapping("/_all")
    @Authorize(merge = false)
    public Flux<DimensionEntity> getAllOrg() {
        return dimensionService
            .createQuery()
            .where(DimensionEntity::getTypeId, orgDimensionTypeId)
            .fetch();
    }

    @GetMapping("/_query")
    @QueryAction
    public Mono<PagerResult<DimensionEntity>> queryDimension(QueryParamEntity entity) {
        return entity
            .toNestQuery(q -> q.where(DimensionEntity::getTypeId, orgDimensionTypeId))
            .execute(Mono::just)
            .as(dimensionService::queryPager);
    }

    @PatchMapping
    @SaveAction
    public Mono<Void> saveOrg(@RequestBody Flux<DimensionEntity> entityFlux) {
        return entityFlux
            .doOnNext(entity -> entity.setTypeId(orgDimensionTypeId))
            .as(dimensionService::save)
            .then();
    }

    @DeleteMapping
    @DeleteAction
    public Mono<Void> deleteOrg(@RequestBody Flux<DimensionEntity> entityFlux) {
        return entityFlux
            .doOnNext(entity -> entity.setTypeId(orgDimensionTypeId))
            .as(dimensionService::save)
            .then();
    }


}
