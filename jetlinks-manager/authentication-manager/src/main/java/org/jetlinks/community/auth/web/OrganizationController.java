package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.OrganizationEntity;
import org.jetlinks.community.auth.service.OrganizationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RequestMapping("/organization")
@RestController
@Resource(id = "organization", name = "部门管理")
@Tag(name = "部门管理")
public class OrganizationController implements ReactiveServiceCrudController<OrganizationEntity, String> {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    private Flux<OrganizationEntity> queryAll() {
        return organizationService.createQuery().fetch();
    }

    private Flux<OrganizationEntity> queryAll(Mono<QueryParamEntity> queryParamEntity) {
        return organizationService.query(queryParamEntity);
    }

    @GetMapping("/_all/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取全部机构信息(树结构)")
    public Flux<OrganizationEntity> getAllOrgTree() {
        return queryAll()
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity.list2tree(list, OrganizationEntity::setChildren));
    }

    @PostMapping("/_all/tree")
    @Authorize(merge = false)
    @Operation(summary = "获取全部机构信息(树结构)")
    public Flux<OrganizationEntity> getAllOrgTree(@RequestBody Mono<QueryParamEntity> query) {
        return queryAll(query)
            .collectList()
            .flatMapIterable(list -> TreeSupportEntity.list2tree(list, OrganizationEntity::setChildren));
    }

    @GetMapping("/_all")
    @Authorize(merge = false)
    @Operation(summary = "获取全部机构信息")
    public Flux<OrganizationEntity> getAllOrg() {
        return queryAll();
    }

    @PostMapping("/_all")
    @Authorize(merge = false)
    @Operation(summary = "获取全部机构信息")
    public Flux<OrganizationEntity> getAllOrg(@RequestBody Mono<QueryParamEntity> query) {
        return queryAll(query);
    }

    @GetMapping("/_query/_children/tree")
    @QueryAction
    @QueryOperation(summary = "查询机构列表(包含子机构)树结构")
    public Mono<List<OrganizationEntity>> queryChildrenTree(@Parameter(hidden = true) QueryParamEntity entity) {
        return organizationService.queryIncludeChildrenTree(entity);
    }

    @GetMapping("/_query/_children")
    @QueryAction
    @QueryOperation(summary = "查询机构列表(包含子机构)")
    public Flux<OrganizationEntity> queryChildren(@Parameter(hidden = true) QueryParamEntity entity) {
        return organizationService.queryIncludeChildren(entity);
    }

    @PostMapping("/{id}/users/_bind")
    @ResourceAction(id = "bind-user", name = "绑定用户")
    @Operation(summary = "绑定用户到机构")
    public Mono<Integer> bindUser(@Parameter(description = "机构ID") @PathVariable String id,
                                  @Parameter(description = "用户ID")
                                  @RequestBody Mono<List<String>> userId) {

        return userId.flatMap(list -> organizationService.bindUser(id, list));

    }

    @PostMapping("/{id}/users/_unbind")
    @ResourceAction(id = "unbind-user", name = "解绑用户")
    @Operation(summary = "从机构解绑用户")
    public Mono<Integer> unbindUser(@Parameter(description = "机构ID") @PathVariable String id,
                                    @Parameter(description = "用户ID")
                                    @RequestBody Mono<List<String>> userId) {
        return userId.flatMap(list -> organizationService.unbindUser(id, list));
    }

    @Override
    public ReactiveCrudService<OrganizationEntity, String> getService() {
        return organizationService;
    }
}
