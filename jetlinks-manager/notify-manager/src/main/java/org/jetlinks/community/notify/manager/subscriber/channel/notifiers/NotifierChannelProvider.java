package org.jetlinks.community.notify.manager.subscriber.channel.notifiers;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.validator.ValidatorUtils;
import org.jetlinks.core.Values;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.manager.entity.Notification;
import org.jetlinks.community.notify.manager.subscriber.channel.NotifyChannel;
import org.jetlinks.community.notify.manager.subscriber.channel.NotifyChannelProvider;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@AllArgsConstructor
public abstract class NotifierChannelProvider implements NotifyChannelProvider {

    private final NotifierManager notifierManager;

    protected abstract NotifyType getNotifyType();

    @Override
    public final String getId() {
        return "notifier-" + getNotifyType().getId();
    }

    @Override
    public String getName() {
        return getNotifyType().getName();
    }

    @Override
    public Mono<NotifyChannel> createChannel(Map<String, Object> configuration) {
        NotifyChannelConfig config = FastBeanCopier.copy(configuration, new NotifyChannelConfig());
        ValidatorUtils.tryValidate(config);
        return Mono.just(new NotifierChannel(config));
    }

    @Getter
    @Setter
    public static class NotifyChannelConfig {
        @NotBlank
        private String notifierId;
        @NotBlank
        private String templateId;

        /**
         * 通知模版变量,用来填充通知模版中需要的变量信息.如:
         * <pre>{@code
         * {
         *     //发送邮件通知时，邮件通知需要变量sendTo,
         *     //通过将上游变量subscriber作为userId来获取对应的收件人信息
         *     "sendTo":{
         *         "source":"relation",
         *         "relation":{
         *              "objectSource":{"source":"upper","upperKey":"subscriber"},
         *              "objectType":"user"
         *         }
         *     }
         * }
         * }</pre>
         *
         * @see org.jetlinks.community.notify.email.embedded.EmailTemplate
         */
        private Map<String, Object> variables;
    }

    @AllArgsConstructor
    class NotifierChannel implements NotifyChannel {

        private final NotifyChannelConfig config;

        private Map<String, Object> createVariable(Notification notification) {
            Map<String, Object> vars = Maps.newHashMapWithExpectedSize(32);

            //消息内容
            vars.put("message", notification.getMessage());
            //通知详情数据
            vars.put("detail", notification.getDetail());
            //通知详情数据-json
            vars.put("detailJson", JSON.toJSONString(notification.getDetail()));
            //编码
            vars.put("code", notification.getCode());
            //订阅者ID
            vars.put("subscriber", notification.getSubscriber());
            //订阅主题名称
            vars.put("topic", notification.getTopicName());
            //通知时间
            vars.put("notifyTime", notification.getNotifyTime());

            if (MapUtils.isNotEmpty(config.variables)) {
                vars.putAll(config.variables);
            }

            return vars;

        }

        @Override
        public Mono<Void> sendNotify(Notification notification) {
            return notifierManager
                .getNotifier(getNotifyType(), config.notifierId)
                .flatMap(notifier -> notifier.send(config.templateId, Values.of(createVariable(notification))));
        }

        @Override
        public void dispose() {

        }
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
