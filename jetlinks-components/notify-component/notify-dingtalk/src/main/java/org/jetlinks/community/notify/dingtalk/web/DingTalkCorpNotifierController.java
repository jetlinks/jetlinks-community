package org.jetlinks.community.notify.dingtalk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyConfigManager;
import org.jetlinks.community.notify.annotation.NotifierResource;
import org.jetlinks.community.notify.dingtalk.corp.DingTalkNotifier;
import org.jetlinks.community.notify.dingtalk.corp.DingTalkProperties;
import org.jetlinks.community.notify.dingtalk.corp.request.GetDepartmentRequest;
import org.jetlinks.community.notify.dingtalk.corp.request.GetUserIdByUnionIdRequest;
import org.jetlinks.community.notify.dingtalk.corp.request.GetUserInfoRequest;
import org.jetlinks.community.notify.dingtalk.corp.request.GetUserRequest;
import org.jetlinks.community.service.UserBindService;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.community.notify.dingtalk.corp.CorpDepartment;
import org.jetlinks.community.notify.dingtalk.corp.CorpUser;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notifier/dingtalk/corp")
@NotifierResource
@Tag(name = "钉钉企业消息通知")
@AllArgsConstructor
public class DingTalkCorpNotifierController {

    private final NotifierManager notifierManager;

    private final NotifyConfigManager notifyConfigManager;

    private final UserBindService userBindService;

    @GetMapping("/{configId}/departments")
    @Operation(summary = "获取企业部门信息")
    public Flux<CorpDepartment> getDepartments(@PathVariable String configId,
                                               @RequestParam(defaultValue = "false") boolean fetchChild) {
        return notifierManager
            .getNotifier(DefaultNotifyType.dingTalk, configId)
            .map(notifier -> notifier.unwrap(CommandSupport.class))
            .flatMapMany(support -> support.execute(new GetDepartmentRequest(fetchChild)));
    }

    @GetMapping("/{configId}/departments/tree")
    @Operation(summary = "获取全部企业部门信息(树结构)")
    public Flux<CorpDepartment> getDepartments(@PathVariable String configId) {
        return this
            .getDepartments(configId, true)
            .collectList()
            .flatMapIterable(CorpDepartment::toTree);
    }


    @GetMapping("/{configId}/{departmentId}/users")
    @Operation(summary = "获取企业部门下人员信息")
    public Flux<CorpUser> getDepartmentUsers(@PathVariable String configId,
                                             @PathVariable String departmentId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.dingTalk, configId)
            .map(notifier -> notifier.unwrap(CommandSupport.class))
            .flatMapMany(support -> support.execute(new GetUserRequest(departmentId)));
    }

    @GetMapping("/{configId}/users")
    @Operation(summary = "获取企业部门下全部人员信息")
    public Flux<CorpUser> getDepartmentUsers(@PathVariable String configId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.dingTalk, configId)
            .map(notifier -> notifier.unwrap(CommandSupport.class))
            .flatMapMany(support -> support
                .execute(new GetDepartmentRequest(true))
                .flatMap(department -> support.execute(new GetUserRequest(department.getId())))
            )
            ;
    }

    @GetMapping(value = "/{configId}/oauth2/binding-user-url")
    @Operation(summary = "生成企业用户Oauth2绑定授权url")
    public Mono<String> getUserBindingUrl(@RequestParam(value = "authCode") String redirectUri,
                                          @PathVariable String configId) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(authentication -> notifyConfigManager
                .getNotifyConfig(DefaultNotifyType.dingTalk, configId)
                .map(properties -> UriComponentsBuilder
                    .fromUriString("https://login.dingtalk.com/oauth2/auth")
                    .queryParam("redirect_uri", HttpUtils.urlEncode(redirectUri))
                    .queryParam("response_type", "code")
                    .queryParam("client_id", DingTalkProperties
                        .of(properties)
                        .getAppKey())
                    .queryParam("scope", "openid")
                    .queryParam("state", configId)
                    .queryParam("prompt", "consent")
                    .build()
                    .toString())
            );

    }


    @GetMapping(value = "/oauth2/user-bind-code")
    @Operation(summary = "获取oauth2授权的用户绑定码")
    public Mono<String> getOauth2UserBindCode(@RequestParam(value = "authCode") String authCode,
                                              @RequestParam(value = "configId") String configId) {
        return Authentication
            .currentReactive()
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMap(authentication -> notifierManager
                .getNotifier(DefaultNotifyType.dingTalk, configId)
                .map(notifier -> notifier.unwrap(DingTalkNotifier.class))
                .flatMap(dingTalkNotifier -> dingTalkNotifier
                    .getUserAccessToken(authCode)
                    .flatMap(accessTokenRep -> dingTalkNotifier.execute(new GetUserInfoRequest(accessTokenRep.getAccessToken())))
                    .flatMap(userInfoResponse -> dingTalkNotifier
                        .execute(new GetUserIdByUnionIdRequest(userInfoResponse.getUnionId()))
                        .flatMap(rep -> userBindService
                            .generateBindCode(new UserBindService
                                .UserInfo(userInfoResponse.getUnionId(),
                                          userInfoResponse.getAvatarUrl(),
                                          userInfoResponse.getNick(),
                                          rep.getResult().getUserId(),
                                          authentication.getUser().getId())))
                    )
                ))   ;

    }
}
