package org.jetlinks.community.notify.wechat.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.annotation.NotifierResource;
import org.jetlinks.community.notify.wechat.corp.CorpDepartment;
import org.jetlinks.community.notify.wechat.corp.CorpTag;
import org.jetlinks.community.notify.wechat.corp.CorpUser;
import org.jetlinks.community.notify.wechat.corp.request.GetDepartmentRequest;
import org.jetlinks.community.notify.wechat.corp.request.GetTagRequest;
import org.jetlinks.community.notify.wechat.corp.request.GetUserRequest;
import org.jetlinks.community.notify.wechat.corp.response.GetDepartmentResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetTagResponse;
import org.jetlinks.community.notify.wechat.corp.response.GetUserResponse;
import org.jetlinks.core.command.CommandSupport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/notifier/wechat/corp")
@Tag(name = "企业微信接口")
@AllArgsConstructor
@NotifierResource
public class WechatCoreNotifierController {

    private final NotifierManager notifierManager;

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
}
