package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.jetlinks.community.auth.entity.UserDetail;
import org.jetlinks.community.auth.service.UserDetailService;
import org.jetlinks.community.auth.service.request.SaveUserDetailRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user/detail")
@AllArgsConstructor
@Tag(name = "用户信息接口")
public class UserDetailController {

    private final UserDetailService userDetailService;

    /**
     * 获取当前登录用户详情
     *
     * @return 用户详情
     */
    @GetMapping
    @Operation(summary = "获取当前登录用户详情")
    public Mono<UserDetail> getCurrentLoginUserDetail() {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(autz -> userDetailService.findUserDetail(autz.getUser().getId()));
    }

    /**
     * 保存当前用户详情
     *
     * @return 用户详情
     */
    @PutMapping
    @Operation(summary = "保存当前用户详情")
    public Mono<Void> saveUserDetail(@RequestBody Mono<SaveUserDetailRequest> request) {
        return Authentication
            .currentReactive()
            .zipWith(request)
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(tp2 -> userDetailService.saveUserDetail(tp2.getT1().getUser().getId(), tp2.getT2()));
    }

}