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
    public Mono<Boolean> saveSettings(@RequestBody Flux<AuthorizationSettingDetail> detailFlux) {

        return Authentication
            .currentReactive()
            .flatMap(authentication -> settingService
                .saveDetail(authentication, detailFlux)
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
