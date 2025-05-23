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
