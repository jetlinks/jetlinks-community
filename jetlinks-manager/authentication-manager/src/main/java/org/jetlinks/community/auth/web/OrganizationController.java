package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.rdb.operator.dml.Terms;
import org.hswebframework.web.api.crud.entity.QueryNoPagingOperation;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.service.OrganizationService;
import org.jetlinks.community.auth.service.PositionService;
import org.jetlinks.community.auth.web.response.OrganizationDetail;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@RequestMapping("/organization")
@RestController
@Resource(id = "organization", name = "组织管理")
@Tag(name = "组织管理")
public class OrganizationController implements ReactiveServiceCrudController<OrganizationEntity, String> {

    private final OrganizationService organizationService;
    private final PositionService positionService;

    public OrganizationController(OrganizationService organizationService,
                                  PositionService positionService) {
        this.organizationService = organizationService;
        this.positionService = positionService;
    }

    @GetMapping("/_all/tree")
    @Authorize(merge = false)
    @QueryNoPagingOperation(summary = "获取全部数据(树结构)")
    public Flux<OrganizationEntity> getAllOrgTree(QueryParamEntity query) {
        return organizationService
            .queryResultToTree(query)
            .flatMapIterable(Function.identity());
    }

    @PostMapping("/_all/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取全部数据(树结构)")
    public Flux<OrganizationDetail> getAllOrgTree(@RequestBody Mono<QueryParamEntity> query,
                                                  @RequestParam(required = false, defaultValue = "false") boolean isCount) {
        return query.flatMapMany(param -> organizationService
            .getAllOrgTreeForMember(param, isCount));
    }

    @GetMapping("/_all")
    @Authorize(merge = false)
    @Operation(summary = "获取全部数据")
    public Flux<OrganizationEntity> getAllOrg(QueryParamEntity query) {
        return organizationService
            .query(query.noPaging());
    }

    @PostMapping("/_all")
    @Authorize(merge = false)
    @Operation(summary = "获取全部数据")
    public Flux<OrganizationEntity> getAllOrg(@RequestBody Mono<QueryParamEntity> query) {
        return organizationService
            .query(query.doOnNext(QueryParamEntity::noPaging));
    }

    @GetMapping("/_query/_children/tree")
    @QueryAction
    @QueryOperation(summary = "查询列表(包含子级)树结构")
    public Mono<List<OrganizationEntity>> queryChildrenTree(@Parameter(hidden = true) QueryParamEntity entity) {
        return organizationService.queryIncludeChildrenTree(entity);
    }

    @GetMapping("/_query/_children")
    @QueryAction
    @QueryOperation(summary = "查询列表(包含子级)")
    public Flux<OrganizationEntity> queryChildren(@Parameter(hidden = true) QueryParamEntity entity) {
        return organizationService.queryIncludeChildren(entity);
    }

    @PostMapping("/detail/_query")
    @QueryNoPagingOperation(summary = "获取详情（列表）")
    public Flux<OrganizationDetail> queryDetail(@RequestBody Mono<QueryParamEntity> query) {
        return organizationService
            .query(query)
            .flatMapSequential(e -> {
                OrganizationDetail detail = OrganizationDetail.from(e);
                if (StringUtils.isBlank(e.getPath()) || detail.getLevel() == 1) {
                    detail.setFullName(detail.getName());
                    return Mono.just(detail);
                }
                //查询所有父级填充fullName
                return organizationService
                    .createQuery()
                    .where()
                    //where ? like path and path !='' and path not null
                    .accept(Terms.Like.reversal("path", e.getPath(), false, true))
                    .notEmpty("path")
                    .notNull("path")
                    .not(OrganizationEntity::getId, e.getId())
                    .fetch()
                    .collectList()
                    .filter(CollectionUtils::isNotEmpty)
                    .map(list -> TreeSupportEntity.list2tree(list, OrganizationEntity::setChildren))
                    .doOnNext(parents -> {
                        if (parents.size() == 1) {
                            detail.addParentFullName(parents.get(0));
                        }
                    })
                    .then(Mono.just(detail));
            }, 16, 16);
    }


    @PostMapping("/{id}/position/{positionId}/users/_bind")
    @ResourceAction(id = "bind-user", name = "绑定用户")
    @Operation(summary = "绑定用户职位")
    public Mono<Void> bindPositionUser(
        @Parameter(description = "组织ID") @PathVariable String id,
        @Parameter(description = "职位ID") @PathVariable String positionId,
        @Parameter(description = "用户ID")
        @RequestBody Mono<List<String>> userIdList) {
        return Mono
            .zip(
                positionService.findById(positionId),
                userIdList,
                (position, idList) -> {
                    //职位不属于指定的组织
                    if (!Objects.equals(position.getOrgId(), id)) {
                        return Mono.error(new AccessDenyException.NoStackTrace());
                    }
                    return positionService.bindUser(idList, Collections.singletonList(positionId), false);
                }
            )
            .flatMap(Function.identity())
            .then();
    }

    @PostMapping("/{id}/position/{positionId}/users/_unbind")
    @ResourceAction(id = "unbind-user", name = "解绑用户")
    @Operation(summary = "解绑用户职位")
    public Mono<Void> unbindPositionUser(
        @Parameter(description = "组织ID") @PathVariable String id,
        @Parameter(description = "职位ID") @PathVariable String positionId,
        @Parameter(description = "用户ID")
        @RequestBody Mono<List<String>> userIdList) {
        return Mono.zip(
                       positionService
                           .findById(positionId),
                       userIdList,
                       (position, idList) -> {
                           //职位不属于指定的组织
                           if (!Objects.equals(position.getOrgId(), id)) {
                               return Mono.error(new AccessDenyException.NoStackTrace());
                           }
                           return positionService.unbindUser(idList, Collections.singletonList(positionId));
                       }
                   )
                   .flatMap(Function.identity())
                   .then();
    }


    @PostMapping("/{id}/users/_bind")
    @ResourceAction(id = "bind-user", name = "绑定用户")
    @Operation(summary = "绑定用户")
    public Mono<Integer> bindUser(@Parameter(description = "组织ID") @PathVariable String id,
                                  @Parameter(description = "用户ID")
                                  @RequestBody Mono<List<String>> userId) {

        return userId
            .flatMap(list -> organizationService.bindUser(id, list));

    }


    @PostMapping("/{id}/users/_unbind")
    @ResourceAction(id = "unbind-user", name = "解绑用户")
    @Operation(summary = "解绑用户")
    public Mono<Integer> unbindUser(@Parameter(description = "组织ID") @PathVariable String id,
                                    @Parameter(description = "用户ID")
                                    @RequestBody Mono<List<String>> userId) {
        return userId
            .flatMap(list -> organizationService.unbindUser(list, Collections.singletonList(id)));
    }

    @Override
    public ReactiveCrudService<OrganizationEntity, String> getService() {
        return organizationService;
    }
}
