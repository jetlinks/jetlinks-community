package org.jetlinks.community.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.ResourceAction;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.manager.service.NotifyConfigService;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.core.Values;
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
@AllArgsConstructor
public class NotifierController {

    private final NotifyConfigService configService;

    private final NotifierManager notifierManager;

    private final TemplateManager templateManager;


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
            NotifyType type = NotifyType.of(tem.getTemplate().getType());
            return Mono
                .zip(
                    notifierManager
                        .getNotifier(type, notifierId)
                        .switchIfEmpty(Mono.error(() -> new NotFoundException("error.notifier_does_not_exist", notifierId))),
                    templateManager.createTemplate(type, tem.getTemplate().toTemplateProperties()),
                    (notifier, template) -> notifier.send(template, Values.of(tem.getContext())))
                .flatMap(Function.identity());
        });
    }

    @PostMapping("/{notifierId}/{templateId}/_send")
    @ResourceAction(id = "send", name = "发送通知")
    @Operation(summary = "根据配置和模版ID发送消息通知")
    public Mono<Void> sendNotify(@PathVariable @Parameter(description = "通知配置ID") String notifierId,
                                 @PathVariable @Parameter(description = "通知模版ID") String templateId,
                                 @RequestBody Mono<Map<String, Object>> contextMono) {
        return configService
            .findById(notifierId)
            .flatMap(conf -> Mono
                .zip(
                    notifierManager
                        .getNotifier(NotifyType.of(conf.getType()), notifierId)
                        .switchIfEmpty(Mono.error(() -> new NotFoundException("error.notifier_does_not_exist", notifierId))),
                    contextMono,
                    (notifier, contextMap) -> notifier.send(templateId, Values.of(contextMap))
                )
                .flatMap(Function.identity()));
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
