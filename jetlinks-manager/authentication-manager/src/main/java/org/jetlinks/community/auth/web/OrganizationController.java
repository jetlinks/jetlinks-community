/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.QueryNoPagingOperation;
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
import java.util.function.Function;

@RequestMapping("/organization")
@RestController
@Resource(id = "organization", name = "组织管理")
@Tag(name = "组织管理")
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
    @QueryNoPagingOperation(summary = "获取全部数据(树结构)")
    public Flux<OrganizationEntity> getAllOrgTree(QueryParamEntity query) {
        return organizationService
            .queryResultToTree(query)
            .flatMapIterable(Function.identity());
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

    @PostMapping("/{id}/users/_bind")
    @ResourceAction(id = "bind-user", name = "绑定用户")
    @Operation(summary = "绑定用户")
    public Mono<Integer> bindUser(@Parameter(description = "组织ID") @PathVariable String id,
                                  @Parameter(description = "用户ID")
                                  @RequestBody Mono<List<String>> userId) {

        return userId.flatMap(list -> organizationService.bindUser(id, list));

    }


    @PostMapping("/{id}/users/_unbind")
    @ResourceAction(id = "unbind-user", name = "解绑用户")
    @Operation(summary = "解绑用户")
    public Mono<Integer> unbindUser(@Parameter(description = "组织ID") @PathVariable String id,
                                    @Parameter(description = "用户ID")
                                    @RequestBody Mono<List<String>> userId) {
        return userId.flatMap(list -> organizationService.unbindUser(id, list));
    }

    @Override
    public ReactiveCrudService<OrganizationEntity, String> getService() {
        return organizationService;
    }
}
