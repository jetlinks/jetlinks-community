package org.jetlinks.community.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.core.Values;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.template.TemplateManager;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/notifier")
@Resource(id = "notifier", name = "通知管理")
@Tag(name = "消息通知管理")
public class NotifierController {


    private final NotifierManager notifierManager;

    private final TemplateManager templateManager;

    public NotifierController(NotifierManager notifierManager, TemplateManager templateManager) {
        this.notifierManager = notifierManager;
        this.templateManager = templateManager;
    }

    /**
     * 指定通知器以及模版.发送通知.
     *
     * @param notifierId 通知器ID
     * @param mono       发送请求
     * @return 发送结果
     */
    @PostMapping("/{notifierId}/_send")
    @ResourceAction(id = "send", name = "发送通知")
    @Operation(summary = "发送消息通知")
    public Mono<Void> sendNotify(@PathVariable @Parameter(description = "通知配置ID") String notifierId,
                                 @RequestBody Mono<SendNotifyRequest> mono) {
        return mono.flatMap(tem -> {
            NotifyType type = DefaultNotifyType.valueOf(tem.getTemplate().getType());
            return Mono.zip(
                notifierManager.getNotifier(type, notifierId)
                    .switchIfEmpty(Mono.error(() -> new NotFoundException("通知器[" + notifierId + "]不存在"))),
                templateManager.createTemplate(type, tem.getTemplate().toTemplateProperties()),
                (notifier, template) -> notifier.send(template, Values.of(tem.getContext())))
                .flatMap(Function.identity());
        });
    }

    @Getter
    @Setter
    public static class SendNotifyRequest {

        @NotNull
        @Schema(description = "通知模版")
        private NotifyTemplateEntity template;

        @Schema(description = "上下文数据")
        private Map<String, Object> context = new HashMap<>();
    }

}