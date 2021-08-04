package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
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

@RequestMapping("/organization")
@RestController
@Resource(id = "organization", name = "机构管理")
@Tag(name = "机构管理")
@AllArgsConstructor
public class OrganizationController {
    static String orgDimensionTypeId = "org";

    private final DefaultDimensionService dimensionService;

    private final DefaultDimensionUserService dimensionUserService;

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
        return dimensionService
            .createQuery()
            .where(DimensionEntity::getTypeId, orgDimensionTypeId)
            .fetch();
    }

    @GetMapping("/_query")
    @QueryAction
    @QueryOperation(summary = "查询结构列表")
    public Mono<PagerResult<DimensionEntity>> queryDimension(@Parameter(hidden = true) QueryParamEntity entity) {
        return entity
            .toNestQuery(q -> q.where(DimensionEntity::getTypeId, orgDimensionTypeId))
            .execute(Mono::just)
            .as(dimensionService::queryPager);
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
                                  @RequestBody Mono<List<String>> userIdStream) {

        return userIdStream
            .flatMapIterable(Function.identity())
            .map(userId -> {
                DimensionUserEntity userEntity = new DimensionUserEntity();
                userEntity.setUserId(userId);
                userEntity.setUserName(userId);
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
                                    @RequestBody Mono<List<String>> userIdStream) {
        return userIdStream
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(newUserIdList -> dimensionUserService
                .createDelete()
                .where(DimensionUserEntity::getDimensionTypeId, orgDimensionTypeId)
                .in(DimensionUserEntity::getUserId, newUserIdList)
                .and(DimensionUserEntity::getDimensionId, id)
                .execute());
    }


}
