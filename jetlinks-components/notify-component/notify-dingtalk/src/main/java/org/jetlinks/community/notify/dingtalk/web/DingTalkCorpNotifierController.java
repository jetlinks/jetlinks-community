package org.jetlinks.community.notify.dingtalk.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.annotation.NotifierResource;
import org.jetlinks.community.notify.dingtalk.corp.request.GetDepartmentRequest;
import org.jetlinks.community.notify.dingtalk.corp.request.GetUserRequest;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.community.notify.dingtalk.corp.CorpDepartment;
import org.jetlinks.community.notify.dingtalk.corp.CorpUser;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/notifier/dingtalk/corp")
@NotifierResource
@Tag(name = "钉钉企业消息通知")
@AllArgsConstructor
public class DingTalkCorpNotifierController {

    private final NotifierManager notifierManager;

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

}
