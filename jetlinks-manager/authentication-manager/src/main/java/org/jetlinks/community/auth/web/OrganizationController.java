package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.system.authorization.api.entity.DimensionEntity;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionService;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequestMapping("/organization")
@RestController
@Resource(id = "organization", name = "机构管理")
@Tag(name = "机构管理")
public class OrganizationController {
    static String orgDimensionTypeId = "org";
    @Autowired
    private DefaultDimensionService dimensionService;

    @Autowired
    private DefaultDimensionUserService dimensionUserService;


    public OrganizationController(DefaultDimensionService dimensionService) {
        this.dimensionService = dimensionService;
    }

    @GetMapping("/_all/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取全部机构信息(树结构)")
    public Flux<DimensionEntity> getAllOrgTree() {
        return getAllOrg()
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity.list2tree(list, DimensionEntity::setChildren));
    }

    @GetMapping("/_all")
    @Authorize(merge = false)
    @Operation(summary = "获取全部机构信息")
    public Flux<DimensionEntity> getAllOrg() {
        return Authentication
            .currentReactive()
            .flatMapMany(auth -> {
                List<String> list = auth.getDimensions(orgDimensionTypeId)
                                        .stream()
                                        .map(Dimension::getId)
                                        .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(list)) {
                    return dimensionService.findById(list);
                }
                return dimensionService
                    .createQuery()
                    .where(DimensionEntity::getTypeId, orgDimensionTypeId)
                    .fetch();
            });
    }

    @GetMapping("/_query")
    @QueryAction
    @QueryOperation(summary = "查询机构列表")
    public Mono<PagerResult<DimensionEntity>> queryDimension(@Parameter(hidden = true) QueryParamEntity entity) {
        return entity
            .toNestQuery(q -> q.where(DimensionEntity::getTypeId, orgDimensionTypeId))
            .execute(Mono::just)
            .as(dimensionService::queryPager);
    }

    @GetMapping("/_query/_children/tree")
    @QueryAction
    @QueryOperation(summary = "查询机构列表(包含子机构)树结构")
    public Mono<List<DimensionEntity>> queryChildrenTree(@Parameter(hidden = true) QueryParamEntity entity) {
        return entity
            .toNestQuery(q -> q.where(DimensionEntity::getTypeId, orgDimensionTypeId))
            .execute(dimensionService::queryIncludeChildrenTree);
    }

    @GetMapping("/_query/_children")
    @QueryAction
    @QueryOperation(summary = "查询机构列表(包含子机构)")
    public Flux<DimensionEntity> queryChildren(@Parameter(hidden = true) QueryParamEntity entity) {
        return entity
            .toNestQuery(q -> q.where(DimensionEntity::getTypeId, orgDimensionTypeId))
            .execute(dimensionService::queryIncludeChildren);
    }

    @PostMapping
    @CreateAction
    @Operation(summary = "新增机构信息")
    public Mono<Void> addOrg(@RequestBody Flux<DimensionEntity> entityFlux) {
        return entityFlux
            .doOnNext(entity -> entity.setTypeId(orgDimensionTypeId))
            .as(dimensionService::insert)
            .then();
    }

    @PutMapping("/{id}")
    @SaveAction
    @Operation(summary = "更新机构信息")
    public Mono<Void> updateOrg(@PathVariable String id, @RequestBody Mono<DimensionEntity> entityMono) {
        return entityMono
            .doOnNext(entity -> {
                entity.setTypeId(orgDimensionTypeId);
                entity.setId(id);
            })
            .as(payload -> dimensionService.updateById(id, payload))
            .then();
    }

    @PatchMapping
    @SaveAction
    @Operation(summary = "保存机构信息")
    public Mono<Void> saveOrg(@RequestBody Flux<DimensionEntity> entityFlux) {
        return entityFlux
            .doOnNext(entity -> entity.setTypeId(orgDimensionTypeId))
            .as(dimensionService::save)
            .then();
    }

    @DeleteMapping("/{id}")
    @DeleteAction
    @Operation(summary = "删除机构信息")
    public Mono<Void> deleteOrg(@PathVariable String id) {
        return dimensionService
            .deleteById(Mono.just(id))
            .then();
    }

    @PostMapping("/{id}/users/_bind")
    @ResourceAction(id = "bind-user", name = "绑定用户")
    @Operation(summary = "绑定用户到机构")
    public Mono<Integer> bindUser(@Parameter(description = "机构ID") @PathVariable String id,
                                  @Parameter(description = "用户ID")
                                  @RequestBody Mono<List<String>> userId) {

        return userId
            .flatMapIterable(Function.identity())
            .map(uId -> {
                DimensionUserEntity userEntity = new DimensionUserEntity();
                userEntity.setUserId(uId);
                userEntity.setUserName(uId);
                userEntity.setDimensionId(id);
                userEntity.setDimensionTypeId(orgDimensionTypeId);
                userEntity.setDimensionName(orgDimensionTypeId);
                return userEntity;
            })
            .as(dimensionUserService::save)
            .map(SaveResult::getTotal);

    }

    @PostMapping("/{id}/users/_unbind")
    @ResourceAction(id = "unbind-user", name = "解绑用户")
    @Operation(summary = "从机构解绑用户")
    public Mono<Integer> unbindUser(@Parameter(description = "机构ID") @PathVariable String id,
                                    @Parameter(description = "用户ID")
                                    @RequestBody Mono<List<String>> userId) {
        return userId
            .flatMap(newUserIdList -> dimensionUserService
                .createDelete()
                .where(DimensionUserEntity::getDimensionTypeId, orgDimensionTypeId)
                .in(DimensionUserEntity::getUserId, newUserIdList)
                .and(DimensionUserEntity::getDimensionId, id)
                .execute());
    }


}
