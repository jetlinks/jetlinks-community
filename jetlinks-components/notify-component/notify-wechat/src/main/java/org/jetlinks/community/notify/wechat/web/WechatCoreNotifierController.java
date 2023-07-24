package org.jetlinks.community.notify.wechat.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyConfigManager;
import org.jetlinks.community.notify.annotation.NotifierResource;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.wechat.corp.*;
import org.jetlinks.community.notify.wechat.corp.request.*;
import org.jetlinks.community.notify.wechat.corp.response.GetDepartmentResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetTagResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetUserDetailResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetUserResponse;
import org.jetlinks.community.service.UserBindService;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.message.codec.http.HttpUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/notifier/wechat/corp")
@Tag(name = "企业微信接口")
@AllArgsConstructor
@NotifierResource
public class WechatCoreNotifierController {

    private final NotifierManager notifierManager;

    private final NotifyConfigManager notifyConfigManager;

    private final UserBindService userBindService;

    private final TemplateManager templateManager;

    @GetMapping("/{configId}/tags")
    @Operation(summary = "获取微信企业标签列表")
    public Flux<CorpTag> getTagList(@PathVariable String configId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.weixin, configId)
            .map(notifier -> notifier.unwrap(CommandSupport.class))
            .flatMap(notifier -> notifier.execute(new GetTagRequest()))
            .flatMapIterable(GetTagResponse::getTagList);
    }

    @Operation(summary = "获取微信企业部门列表")
    @GetMapping("/{configId}/departments")
    public Flux<CorpDepartment> getDepartmentList(@PathVariable String configId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.weixin, configId)
            .map(notifier -> notifier.unwrap(CommandSupport.class))
            .flatMap(notifier -> notifier.execute(new GetDepartmentRequest()))
            .flatMapIterable(GetDepartmentResponse::getDepartment);
    }

    @Operation(summary = "获取微信企业成员列表")
    @GetMapping("/{configId}/{departmentId}/users")
    public Flux<CorpUser> getCorpUserList(@PathVariable String configId,
                                          @PathVariable String departmentId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.weixin, configId)
            .map(notifier -> notifier.unwrap(CommandSupport.class))
            .flatMap(notifier -> notifier.execute(new GetUserRequest(departmentId, false)))
            .flatMapIterable(GetUserResponse::getUserList);
    }

    @Operation(summary = "获取微信企业全部成员列表")
    @GetMapping("/{configId}/users")
    public Flux<CorpUser> getCorpAllUserList(@PathVariable String configId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.weixin, configId)
            .map(notifier -> notifier.unwrap(CommandSupport.class))
            .flatMapMany(notifier -> notifier
                .execute(new GetDepartmentRequest())
                .flatMapIterable(GetDepartmentResponse::getDepartment)
                .flatMap(department -> notifier.execute(new GetUserRequest(department.getId(), false))))
            .flatMapIterable(GetUserResponse::getUserList);
    }

    @Operation(summary = "生成企业用户Oauth2绑定授权url")
    @GetMapping(value = "/{configId}/{templateId}/oauth2/binding-user-url")
    public Mono<String> getUserBindingUrl(@RequestParam @Parameter(description = "重定向地址") String redirectUri,
                                          @PathVariable @Parameter(description = "通知配置ID") String configId,
                                          @PathVariable @Parameter(description = "通知模板ID") String templateId) {
        return Mono
            .zip(
                notifyConfigManager
                    .getNotifyConfig(DefaultNotifyType.weixin, configId)
                    .map(WechatCorpProperties::of),
                templateManager
                    .getTemplate(DefaultNotifyType.weixin, templateId)
                    .cast(WechatMessageTemplate.class)
            )
            .map(tp2 -> UriComponentsBuilder
                .fromHttpUrl("https://login.work.weixin.qq.com/wwlogin/sso/login")
                .queryParam("login_type", "CorpApp")
                .queryParam("appid", tp2.getT1().getCorpId())
                .queryParam("redirect_uri", HttpUtils.urlEncode(redirectUri))
                .queryParam("state", IDGenerator.UUID.generate())
                .queryParam("agentid", tp2.getT2().getAgentId())
                .build(true)
                .toString()
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
                .getNotifier(DefaultNotifyType.weixin, configId)
                .map(notifier -> notifier.unwrap(WechatCorpNotifier.class))
                // 查询用户详情
                .flatMap(notifier -> this.getUserDetail(notifier, authCode))
                // 创建绑定码
                .flatMap(userDetailResponse -> userBindService
                    .generateBindCode(new UserBindService.UserInfo(
                        userDetailResponse.getUnionId(),
                        userDetailResponse.getAvatar(),
                        StringUtils.hasText(userDetailResponse.getName()) ?
                            userDetailResponse.getName() : userDetailResponse.getAlias(),
                        userDetailResponse.getId(),
                        authentication.getUser().getId()
                    )))
            );
    }

    private Mono<GetUserDetailResponse> getUserDetail(WechatCorpNotifier notifier,
                                                      String code) {
        return notifier
            // 获取访问用户身份
            .execute(new GetUserInfoRequest(code))
            // 校验是否为企业成员
            .flatMap(userInfoResponse -> StringUtils.hasText(userInfoResponse.getUserId()) ?
                Mono.just(userInfoResponse) :
                Mono.error(() -> new BusinessException("error.wx_bind_only_support_corp_member")))
            .flatMap(userInfoResponse -> notifier.execute(new GetUserDetailRequest(userInfoResponse.getUserId())));
    }

}
