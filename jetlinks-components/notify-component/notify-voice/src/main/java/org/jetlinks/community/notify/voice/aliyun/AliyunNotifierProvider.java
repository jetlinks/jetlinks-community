package org.jetlinks.community.notify.voice.aliyun;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.notify.*;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.community.notify.voice.VoiceProvider;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * <a href="https://help.aliyun.com/document_detail/114035.html?spm=a2c4g.11186623.6.561.3d1b3c2dGMXAmk">
 * 阿里云语音通知服务
 * </a>
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
@AllArgsConstructor
public class AliyunNotifierProvider implements NotifierProvider, TemplateProvider {

    private TemplateManager templateManager;

    @Nonnull
    @Override
    public Provider getProvider() {
        return VoiceProvider.aliyun;
    }

    public static final DefaultConfigMetadata templateConfig = new DefaultConfigMetadata("阿里云语音模版",
            "https://help.aliyun.com/document_detail/114035.html?spm=a2c4g.11186623.6.561.3d1b3c2dGMXAmk")
            .add("ttsCode", "模版ID", "ttsCode", new StringType())
            .add("calledShowNumbers", "被叫显号", "", new StringType())
            .add("CalledNumber", "被叫号码", "", new StringType())
            .add("PlayTimes", "播放次数", "", new IntType());

    public static final DefaultConfigMetadata notifierConfig = new DefaultConfigMetadata("阿里云通知配置",
            "https://help.aliyun.com/document_detail/114035.html?spm=a2c4g.11186623.6.561.3d1b3c2dGMXAmk")
            .add("regionId", "regionId", "regionId", new StringType())
            .add("accessKeyId", "accessKeyId", "", new StringType())
            .add("secret", "secret", "", new StringType());

    @Override
    public ConfigMetadata getTemplateConfigMetadata() {
        return templateConfig;
    }

    @Override
    public ConfigMetadata getNotifierConfigMetadata() {
        return notifierConfig;
    }

    @Override
    public Mono<AliyunVoiceTemplate> createTemplate(TemplateProperties properties) {
        return Mono.fromCallable(() -> new AliyunVoiceTemplate().with(properties).validate())
            .as(LocaleUtils::transform);
    }

    @Nonnull
    @Override
    public NotifyType getType() {
        return DefaultNotifyType.voice;
    }

    @Nonnull
    @Override
    public Mono<AliyunVoiceNotifier> createNotifier(@Nonnull NotifierProperties properties) {
        return Mono.fromSupplier(() -> new AliyunVoiceNotifier(properties, templateManager))
            .as(LocaleUtils::transform);
    }
}
