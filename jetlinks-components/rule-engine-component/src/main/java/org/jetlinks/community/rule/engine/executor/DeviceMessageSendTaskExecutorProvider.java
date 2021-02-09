package org.jetlinks.community.rule.engine.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.utils.ExpressionUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
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
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            Map<String, Object> ctx = RuleDataHelper.toContextMap(input);
            return devices
                .filterWhen(DeviceOperator::isOnline)
                .publishOn(Schedulers.parallel())
                .flatMap(device -> config.doSend(ctx, device))
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
        public Publisher<DeviceMessageReply> doSend(Map<String, Object> ctx, DeviceOperator device) {
            Map<String, Object> message = new HashMap<>(this.message);
            message.put("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
            message.put("deviceId", device.getDeviceId());
            return Mono
                .justOrEmpty(MessageType.convertMessage(message))
                .cast(RepayableDeviceMessage.class)
                .map(msg -> applyMessageExpression(ctx, msg))
                .doOnNext(msg -> msg.addHeader(Headers.async, async))
                .flatMapMany(msg -> device.messageSender().send(Mono.just(msg)));
        }

        private ReadPropertyMessage applyMessageExpression(Map<String, Object> ctx, ReadPropertyMessage message) {
            List<String> properties = message.getProperties();

            if (!CollectionUtils.isEmpty(properties)) {
                message.setProperties(
                    properties.stream().map(prop -> ExpressionUtils.analytical(prop, ctx, "spel")).collect(Collectors.toList())
                );
            }

            return message;
        }

        private WritePropertyMessage applyMessageExpression(Map<String, Object> ctx, WritePropertyMessage message) {
            Map<String, Object> properties = message.getProperties();

            if (!CollectionUtils.isEmpty(properties)) {
                message.setProperties(
                    properties.entrySet()
                        .stream()
                        .map(prop -> Tuples.of(prop.getKey(), ExpressionUtils.analytical(String.valueOf(prop.getValue()), ctx, "spel")))
                        .collect(Collectors.toMap(Tuple2::getT1, Tuple2::getT2))
                );
            }

            return message;
        }

        private FunctionInvokeMessage applyMessageExpression(Map<String, Object> ctx, FunctionInvokeMessage message) {
            List<FunctionParameter> inputs = message.getInputs();

            if (!CollectionUtils.isEmpty(inputs)) {
                for (FunctionParameter input : inputs) {
                    input.setValue(ExpressionUtils.analytical(String.valueOf(input.getValue()), ctx, "spel"));
                }
            }

            return message;
        }

        private RepayableDeviceMessage<?> applyMessageExpression(Map<String, Object> ctx, RepayableDeviceMessage<?> message) {
            if (message instanceof ReadPropertyMessage) {
                return applyMessageExpression(ctx, ((ReadPropertyMessage) message));
            }
            if (message instanceof WritePropertyMessage) {
                return applyMessageExpression(ctx, ((WritePropertyMessage) message));
            }
            if (message instanceof FunctionInvokeMessage) {
                return applyMessageExpression(ctx, ((FunctionInvokeMessage) message));
            }
            return message;
        }

        public void validate() {
            if (StringUtils.isEmpty(deviceId) && StringUtils.isEmpty(productId)) {
                throw new IllegalArgumentException("deviceId和productId不能同时为空");
            }
            MessageType.convertMessage(message).orElseThrow(() -> new IllegalArgumentException("不支持的消息格式"));
        }

    }
}
