package org.jetlinks.community.rule.engine.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.FunctionTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

@Component
@AllArgsConstructor
public class DeviceMessageSendTaskExecutorProvider implements TaskExecutorProvider {

    private final DeviceRegistry registry;

    @Override
    public String getExecutor() {
        return "device-message-sender";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DeviceMessageSendTaskExecutor(context));
    }

    class DeviceMessageSendTaskExecutor extends FunctionTaskExecutor {

        private Config config;

        public DeviceMessageSendTaskExecutor(ExecutionContext context) {
            super("发送设备消息", context);
            validate();
            reload();
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            Flux<DeviceOperator> devices = StringUtils.hasText(config.getDeviceId())
                ? registry.getDevice(config.getDeviceId()).flux()
                : registry.getProduct(config.getProductId()).flatMapMany(DeviceProductOperator::getDevices);

            return devices
                .filterWhen(DeviceOperator::isOnline)
                .publishOn(Schedulers.parallel())
                .flatMap(config::doSend)
                .onErrorResume(error -> context.onError(error, input).then(Mono.empty()))
                .map(reply -> input.newData(reply.toJson()))
                ;
        }

        @Override
        public void validate() {
            if (CollectionUtils.isEmpty(context.getJob().getConfiguration())) {
                throw new IllegalArgumentException("配置不能为空");
            }
            Config config = FastBeanCopier.copy(context.getJob().getConfiguration(), new Config());
            config.validate();
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new Config());
        }


    }


    @Getter
    @Setter
    public static class Config {

        //设备ID
        private String deviceId;

        //产品ID
        private String productId;

        private Map<String, Object> message;

        private boolean async;

        @SuppressWarnings("all")
        public Publisher<DeviceMessageReply> doSend(DeviceOperator device) {
            Map<String, Object> message = new HashMap<>(this.message);
            message.put("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
            message.put("deviceId", device.getDeviceId());
            return Mono
                .justOrEmpty(MessageType.convertMessage(message))
                .cast(RepayableDeviceMessage.class)
                .doOnNext(msg -> msg.addHeader(Headers.async, async))
                .flatMapMany(msg -> device.messageSender().send(Mono.just(msg)));
        }

        public void validate() {
            if (StringUtils.isEmpty(deviceId) && StringUtils.isEmpty(productId)) {
                throw new IllegalArgumentException("deviceId和productId不能同时为空");
            }
            MessageType.convertMessage(message).orElseThrow(() -> new IllegalArgumentException("不支持的消息格式"));
        }

    }
}
