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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.RoleGroupEntity;
import org.jetlinks.community.auth.service.RoleGroupService;
import org.jetlinks.community.auth.web.response.RoleGroupDetailTree;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/role/group")
@Resource(id = "role-group", name = "角色组管理")
@AllArgsConstructor
@Getter
@Tag(name = "角色组管理")
public class RoleGroupController implements ReactiveServiceCrudController<RoleGroupEntity, String> {

    private final RoleGroupService roleGroupService;

    @PostMapping("/detail/_query/tree")
    @Operation(summary = "查询分组及角色(树状)")
    @QueryAction
    public Flux<RoleGroupDetailTree> queryDetailTree(@RequestParam(defaultValue = "false") @Parameter(description = "true:query为角色条件,false:query为分组条件") boolean queryByRole,
                                                     @RequestBody Mono<QueryParamEntity> query) {

        return Mono
            .zip(queryByRole ? query : Mono.just(new QueryParamEntity()),
                 queryByRole ? Mono.just(new QueryParamEntity()) : query)
            .flatMapMany(tp2 -> roleGroupService.queryDetailTree(tp2.getT2(), tp2.getT1()));
    }

    @Override
    public ReactiveCrudService<RoleGroupEntity, String> getService() {
        return roleGroupService;
    }
}
