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
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionUserService;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.enums.UserEntityType;
import org.jetlinks.community.auth.enums.UserEntityTypes;
import org.jetlinks.community.auth.service.UserDetailService;
import org.jetlinks.community.auth.service.request.SaveUserDetailRequest;
import org.jetlinks.community.auth.service.request.SaveUserRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user/detail")
@AllArgsConstructor
@Tag(name = "用户信息接口")
@Resource(id = "user", name = "系统用户", group = "system")
public class UserDetailController {

    private final UserDetailService userDetailService;
    private final DefaultDimensionUserService dimensionUserService;

    @PostMapping("/_create")
    @SaveAction
    @Operation(summary = "创建用户")
    @Transactional
    public Mono<String> createUser(@RequestBody Mono<SaveUserRequest> body) {
        return body
            .flatMap(userDetailService::saveUser);
    }

    @PutMapping("/{userId}/_update")
    @SaveAction
    @Operation(summary = "修改用户")
    public Mono<String> updateUser(@PathVariable String userId,
                                   @RequestBody Mono<SaveUserRequest> body) {
        return body
            .doOnNext(request -> {
                if (request.getUser() != null) {
                    request.getUser().setId(userId);
                }
            })
            .flatMap(userDetailService::saveUser);
    }

    @GetMapping("/{userId}")
    @QueryAction
    @Operation(summary = "获取用户详情信息")
    public Mono<UserDetail> getUserDetail(@PathVariable String userId) {
        return userDetailService.findUserDetail(userId);
    }

    @PostMapping("/_query")
    @QueryAction
    @Operation(summary = "分页获取用户详情")
    public Mono<PagerResult<UserDetail>> queryUserDetail(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(userDetailService::queryUserDetail);
    }

    /**
     * 获取当前登录用户详情
     *
     * @return 用户详情
     */
    @GetMapping
    @Operation(summary = "获取当前登录用户详情")
    @Authorize(merge = false)
    public Mono<UserDetail> getCurrentLoginUserDetail() {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException.NoStackTrace::new))
            .flatMap(autz -> userDetailService
                .findUserDetail(autz.getUser().getId())
                .switchIfEmpty(Mono.fromSupplier(() -> new UserDetail().with(autz)))
            );
    }

    /**
     * 保存当前用户详情
     *
     * @return 用户详情
     */
    @PutMapping
    @Operation(summary = "保存当前用户详情")
    @Authorize(merge = false)
    public Mono<Void> saveUserDetail(@RequestBody Mono<SaveUserDetailRequest> request) {
        // todo 个人资料的权限校验
        return Authentication
            .currentReactive()
            .zipWith(request)
            .switchIfEmpty(Mono.error(UnAuthorizedException.NoStackTrace::new))
            .flatMap(tp2 -> userDetailService
                .saveUserDetail(tp2.getT1().getUser().getId(), tp2.getT2()));
    }

    @GetMapping("/types")
    @Operation(summary = "获取所有用户类型")
    public Flux<UserEntityType> getUserEntityTypes() {
        return Flux.fromIterable(UserEntityTypes.getAllType());
    }

}
