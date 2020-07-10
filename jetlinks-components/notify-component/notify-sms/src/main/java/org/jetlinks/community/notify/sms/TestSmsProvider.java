package org.jetlinks.community.notify.sms;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.notify.*;
import org.jetlinks.core.Values;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.notify.template.Template;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Slf4j
@Component
@Profile({"dev", "test"})
public class TestSmsProvider extends AbstractNotifier<PlainTextSmsTemplate> implements NotifierProvider, TemplateProvider, Provider {

    public TestSmsProvider(TemplateManager templateManager) {
        super(templateManager);
    }

    @Override
    @Nonnull
    public NotifyType getType() {
        return DefaultNotifyType.sms;
    }

    @Override
    @Nonnull
    public Provider getProvider() {
        return this;
    }

    @Override
    public Mono<? extends Template> createTemplate(TemplateProperties properties) {
        return Mono.fromSupplier(() -> JSON.parseObject(properties.getTemplate(), PlainTextSmsTemplate.class));
    }

    @Override
    public ConfigMetadata getTemplateConfigMetadata() {
        return PlainTextSmsTemplate.templateConfig;
    }

    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<Void> send(@Nonnull PlainTextSmsTemplate template, @Nonnull Values context) {
        return Mono.fromRunnable(() -> log.info("send sms {} message:{}", template.getSendTo(context.getAllValues()), template.getTextSms(context.getAllValues())));
    }

    @Nonnull
    @Override
    public Mono<Void> close() {
        return Mono.empty();
    }

    @Nonnull
    @Override
    public Mono<TestSmsProvider> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.just(this);
    }

    @Override
    public String getNotifierId() {
        return "test-sms-sender";
    }

    @Override
    public String getId() {
        return "test";
    }

    @Override
    public String getName() {
        return "测试";
    }
}
