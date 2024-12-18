package org.jetlinks.community.notify.voice.aliyun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.voice.VoiceProvider;
import org.jetlinks.core.Values;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class AliyunVoiceNotifier extends AbstractNotifier<AliyunVoiceTemplate> {

    private final IAcsClient client;
    private final int connectTimeout = 1000;
    private final int readTimeout = 5000;

    @Getter
    private String notifierId;

    private String domain = "dyvmsapi.aliyuncs.com";
    private String regionId = "cn-hangzhou";

    public AliyunVoiceNotifier(NotifierProperties profile, TemplateManager templateManager) {
        super(templateManager);
        Map<String, Object> config = profile.getConfiguration();
        DefaultProfile defaultProfile = DefaultProfile.getProfile(
            this.regionId = (String) Objects.requireNonNull(config.get("regionId"), "regionId不能为空"),
            (String) Objects.requireNonNull(config.get("accessKeyId"), "accessKeyId不能为空"),
            (String) Objects.requireNonNull(config.get("secret"), "secret不能为空")
        );
        this.client = new DefaultAcsClient(defaultProfile);
        this.domain = (String) config.getOrDefault("domain", "dyvmsapi.aliyuncs.com");
        this.notifierId = profile.getId();
    }

    public AliyunVoiceNotifier(IClientProfile profile, TemplateManager templateManager) {
        this(new DefaultAcsClient(profile), templateManager);
    }

    public AliyunVoiceNotifier(IAcsClient client, TemplateManager templateManager) {
        super(templateManager);
        this.client = client;
    }

    @Override
    @Nonnull
    public NotifyType getType() {
        return DefaultNotifyType.voice;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return VoiceProvider.aliyun;
    }

    @Override
    @Nonnull
    public Mono<Void> send(@Nonnull AliyunVoiceTemplate template, @Nonnull Values context) {

        return Flux.defer(() -> {
                       if (AliyunVoiceTemplate.TemplateType.voice == template.getTemplateType()) {
                           return convertVoiceRequest(template, context)
                               .flatMap(this::handleRequest);
                       } else {
                           return convertTtsRequest(template, context)
                               .flatMap(this::handleRequest);
                       }
                   }).doOnEach(ReactiveLogger.onError(err -> {
                       log.info("发起语音通知失败", err);
                   })).subscribeOn(Schedulers.boundedElastic())
                   .then();
    }

    @Override
    @Nonnull
    public Mono<Void> close() {
        return Mono.fromRunnable(client::shutdown);
    }

    private Mono<Void> handleRequest(CommonRequest request) {
        try {
            CommonResponse response = client.getCommonResponse(request);

            if (response == null) {
                throw new BusinessException("error.unsupported_voice_notification_type");
            }

            log.info("发起语音通知完成 {}:{}", response.getHttpResponse().getStatus(), response.getData());

            JSONObject json = JSON.parseObject(response.getData());
            if (!"ok".equalsIgnoreCase(json.getString("Code"))) {
                return Mono.error(new BusinessException(json.getString("Message"), json.getString("Code")));
            }
        } catch (Exception e) {
            return Mono.error(e);
        }
        return Mono.empty();
    }


    Flux<CommonRequest> convertVoiceRequest(AliyunVoiceTemplate template, Values context){
        return template
            .getCalledNumber(context.getAllValues())
            .map(calledNumber -> {
                CommonRequest request = convert(template);
                request.putQueryParameter("CalledNumber", calledNumber);
                request.setSysAction("SingleCallByVoice");
                request.putQueryParameter("VoiceCode", template.getTemplateCode());
                return request;
            });
    }

    Mono<CommonRequest> convertTtsRequest(AliyunVoiceTemplate template, Values context){
        return template
            .getCalledNumber(context.getAllValues())
            .next()
            .map(calledNumber -> {
                CommonRequest request = convert(template);
                request.putQueryParameter("CalledNumber", calledNumber);
                request.putQueryParameter("TtsParam", template.createTtsParam(context.getAllValues()));
                request.putQueryParameter("TtsCode", template.getTemplateCode());
                request.setSysAction("SingleCallByTts");
                return request;
            });

    }

    CommonRequest convert(AliyunVoiceTemplate template){
        CommonRequest request = new CommonRequest();
        request.setSysMethod(MethodType.POST);
        request.setSysDomain(domain);
        request.setSysVersion("2017-05-25");
        request.setSysConnectTimeout(connectTimeout);
        request.setSysReadTimeout(readTimeout);
        request.putQueryParameter("RegionId", regionId);
        // 使用公共号池模板时，不填写主叫显号
        if (StringUtils.hasText(template.getCalledShowNumbers())) {
            request.putQueryParameter("CalledShowNumber", template.getCalledShowNumbers());
        }
        request.putQueryParameter("PlayTimes", String.valueOf(template.getPlayTimes()));
        return request;
    }
}
