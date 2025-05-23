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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.auth.entity.RoleDetailInfo;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.service.RoleService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/role")
@Resource(id = "role", name = "角色管理")
@AllArgsConstructor
@Getter
@Tag(name = "角色管理")
public class RoleController implements ReactiveServiceCrudController<RoleEntity, String> {

    private final RoleService service;

    @PostMapping("/{roleId}/users/_bind")
    @Operation(summary = "绑定用户")
    @SaveAction
    public Mono<Void> bindUser(@PathVariable String roleId,
                               @RequestBody Mono<List<String>> userId) {
        return userId
            .flatMap(list -> service.bindUser(list, Collections.singleton(roleId), false));
    }

    @PostMapping("/{roleId}/users/_unbind")
    @Operation(summary = "解绑用户")
    @SaveAction
    public Mono<Void> unbindUser(@PathVariable String roleId,
                                 @RequestBody Mono<List<String>> userId) {
        return userId
            .flatMap(list -> service.unbindUser(list, Collections.singleton(roleId)));
    }
}
