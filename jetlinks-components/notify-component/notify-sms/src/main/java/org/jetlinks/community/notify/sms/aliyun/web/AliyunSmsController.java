package org.jetlinks.community.notify.sms.aliyun.web;

import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.jetlinks.community.notify.annotation.NotifierResource;
import org.jetlinks.community.notify.sms.aliyun.AliyunSmsNotifier;
import org.jetlinks.community.notify.sms.aliyun.expansion.SmsSign;
import org.jetlinks.community.notify.sms.aliyun.expansion.SmsTemplate;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author bestfeng
 */
@RestController
@RequestMapping("/notifier/sms/aliyun")
@NotifierResource
@AllArgsConstructor
public class AliyunSmsController {

    private final NotifierManager notifierManager;


    @GetMapping("/{configId}/signs")
    @QueryAction
    @Operation(summary = "获取短信标签列表")
    public Flux<SmsSign> getAliyunSmsSigns(@PathVariable String configId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.sms, configId)
            .filter(notifier -> notifier.isWrapperFor(AliyunSmsNotifier.class))
            .map(notifier -> notifier.unwrap(AliyunSmsNotifier.class))
            .flatMapMany(AliyunSmsNotifier::getSmsSigns);

    }

    @GetMapping("/{configId}/templates")
    @QueryAction
    @Operation(summary = "获取短信模板列表")
    public Flux<SmsTemplate> getAliyunSmsTemplates(@PathVariable String configId) {
        return notifierManager
            .getNotifier(DefaultNotifyType.sms, configId)
            .filter(notifier -> notifier.isWrapperFor(AliyunSmsNotifier.class))
            .map(notifier -> notifier.unwrap(AliyunSmsNotifier.class))
            .flatMapMany(AliyunSmsNotifier::getSmsTemplates);

    }
}
