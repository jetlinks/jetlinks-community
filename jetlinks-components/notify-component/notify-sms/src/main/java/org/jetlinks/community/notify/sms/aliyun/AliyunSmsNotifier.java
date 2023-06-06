package org.jetlinks.community.notify.sms.aliyun;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsSignListRequest;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsSignListResponse;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsTemplateListRequest;
import com.aliyuncs.dysmsapi.model.v20170525.QuerySmsTemplateListResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.aliyuncs.profile.IClientProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.sms.SmsProvider;
import org.jetlinks.core.Values;
import org.jetlinks.core.trace.FluxTracer;
import org.jetlinks.community.notify.*;
import org.jetlinks.community.notify.sms.aliyun.expansion.AliyunSmsExpansion;
import org.jetlinks.community.notify.sms.aliyun.expansion.SmsSign;
import org.jetlinks.community.notify.sms.aliyun.expansion.SmsTemplate;
import org.jetlinks.community.notify.template.TemplateManager;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AliyunSmsNotifier extends AbstractNotifier<AliyunSmsTemplate> {

    private final IAcsClient client;
    private final int connectTimeout = 1000;
    private final int readTimeout = 5000;

    @Getter
    private String notifierId;

    private String domain = "dysmsapi.aliyuncs.com";
    private String regionId = "cn-hangzhou";

    public AliyunSmsNotifier(NotifierProperties profile, TemplateManager templateManager) {
        super(templateManager);
        regionId = profile
            .getString("regionId")
            .orElseThrow(() -> new IllegalArgumentException("[regionId]不能为空"));

        String accessKeyId = profile
            .getString("accessKeyId")
            .orElseThrow(() -> new IllegalArgumentException("[accessKeyId]不能为空"));
        String secret = profile
            .getString("secret")
            .orElseThrow(() -> new IllegalArgumentException("[secret]不能为空"));

        this.domain = profile.getString("domain", this.domain);

        DefaultProfile defaultProfile = DefaultProfile.getProfile(
            regionId,
            accessKeyId,
            secret
        );
        this.client = new DefaultAcsClient(defaultProfile);
        this.notifierId = profile.getId();
    }

    public AliyunSmsNotifier(IClientProfile profile, TemplateManager templateManager) {
        this(new DefaultAcsClient(profile), templateManager);
    }

    public AliyunSmsNotifier(IAcsClient client, TemplateManager templateManager) {
        super(templateManager);
        this.client = client;
    }

    @Override
    @Nonnull
    public NotifyType getType() {
        return DefaultNotifyType.sms;
    }

    @Nonnull
    @Override
    public Provider getProvider() {
        return SmsProvider.aliyunSms;
    }


    @Override
    @Nonnull
    public Mono<Void> send(@Nonnull AliyunSmsTemplate template, @Nonnull Values context) {
        Map<String, Object> ctx = context.getAllValues();

        return template
            .getPhoneNumber(ctx)
            .collect(Collectors.joining(","))
            .filter(StringUtils::hasText)
            .flatMap(phoneNumber -> {
                try {
                    CommonRequest request = new CommonRequest();
                    request.setSysMethod(MethodType.POST);
                    request.setSysDomain(domain);
                    request.setSysVersion("2017-05-25");
                    request.setSysAction("SendSms");
                    request.setSysConnectTimeout(connectTimeout);
                    request.setSysReadTimeout(readTimeout);
                    request.putQueryParameter("RegionId", regionId);
                    request.putQueryParameter("PhoneNumbers", phoneNumber);
                    request.putQueryParameter("SignName", template.getSignName(ctx));
                    request.putQueryParameter("TemplateCode", template.getCode(ctx));
                    request.putQueryParameter("TemplateParam", template.createTtsParam(ctx));

                    CommonResponse response = client.getCommonResponse(request);

                    log.info("发送短信通知完成 {}:{}", response.getHttpResponse().getStatus(), response.getData());

                    JSONObject json = JSON.parseObject(response.getData());
                    if (!"ok".equalsIgnoreCase(json.getString("Code"))) {
                        return Mono.error(new BusinessException(json.getString("Message"), json.getString("Code")));
                    }
                } catch (Exception e) {
                    return Mono.error(e);
                }
                return Mono.empty();
            })
            .doOnEach(ReactiveLogger.onError(err -> {
                log.info("发送短信通知失败", err);
            }))
            .subscribeOn(Schedulers.boundedElastic())
            .then();
    }

    @Override
    @Nonnull
    public Mono<Void> close() {
        return Mono.fromRunnable(client::shutdown);
    }


    /**
     * @return 阿里云短信扩展信息
     */
    public Mono<AliyunSmsExpansion> getSmsExpansion() {
        return Mono
            .zip(
                getSmsSigns()
                    .collectList(),
                getSmsTemplates()
                    .collectList(),
                AliyunSmsExpansion::of
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * @return 短信签名集合
     */
    public Flux<SmsSign> getSmsSigns() {
        return doQuerySmsSigns(new AtomicInteger(0), 50)
            .flatMapIterable(Function.identity())
            .map(SmsSign::of)
            .as(FluxTracer.create("/aliyun/sms/sign"))
            .onErrorResume(err -> Mono.empty());
    }


    /**
     * @return 短信模板集合
     */
    public Flux<SmsTemplate> getSmsTemplates() {
        return doQuerySmsTemplates(new AtomicInteger(0), 50)
            .flatMapIterable(Function.identity())
            .map(SmsTemplate::of)
            .as(FluxTracer.create("/aliyun/sms/template"))
            .onErrorResume(err -> Mono.empty());
    }


    public Flux<List<QuerySmsSignListResponse.QuerySmsSignDTO>> doQuerySmsSigns(AtomicInteger pageIndex, int pageSize) {
        QuerySmsSignListRequest request = new QuerySmsSignListRequest();
        request.setPageSize(pageSize);
        request.setPageIndex(pageIndex.incrementAndGet());
        return Mono
            .fromCallable(() -> client.getAcsResponse(request).getSmsSignList())
            .expand(dtos -> {
                if (dtos.size() == pageSize){
                    return doQuerySmsSigns(pageIndex, pageSize);
                }
                return Flux.empty();
            });
    }

    public Flux<List<QuerySmsTemplateListResponse.SmsStatsResultDTO>> doQuerySmsTemplates(AtomicInteger pageIndex, int pageSize) {
        QuerySmsTemplateListRequest request = new QuerySmsTemplateListRequest();
        request.setPageSize(pageSize);
        request.setPageIndex(pageIndex.incrementAndGet());
        return Mono
            .fromCallable(() -> client.getAcsResponse(request).getSmsTemplateList())
            .expand(dtos -> {
                if (dtos.size() == pageSize){
                    return doQuerySmsTemplates(pageIndex, pageSize);
                }
                return Flux.empty();
            });
    }
}
