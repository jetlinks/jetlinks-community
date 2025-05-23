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
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.auth.service.AuthorizationSettingDetailService;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/autz-setting/detail")
@Authorize
@Resource(
    id = "autz-setting",
    name = "权限分配",
    group = "system"
)
@AllArgsConstructor
@Tag(name = "权限分配")
public class AuthorizationSettingDetailController {

    private final AuthorizationSettingDetailService settingService;

    @PostMapping("/_save")
    @SaveAction
    @Operation(summary = "赋权")
    public Mono<Boolean> saveSettings(@RequestBody Mono<List<AuthorizationSettingDetail>> detailMono) {

        return Authentication
            .currentReactive()
            .flatMap(authentication -> detailMono
                .flatMapMany(Flux::fromIterable)
                .as(flux -> settingService.saveDetail(authentication, flux))
                .thenReturn(true)
            );
    }

    @GetMapping("/{targetType}/{target}")
    @SaveAction
    @Operation(summary = "获取权限详情")
    public Mono<AuthorizationSettingDetail> getSettings(@PathVariable @Parameter(description = "权限类型") String targetType,
                                                        @PathVariable @Parameter(description = "权限类型对应数据ID") String target) {

        return settingService
            .getSettingDetail(targetType, target)
            ;
    }

}
