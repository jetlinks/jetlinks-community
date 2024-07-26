package org.jetlinks.community.rule.engine.executor;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessage;
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

import java.time.Duration;
import java.util.*;
import java.util.function.Function;


@AllArgsConstructor
@Component
public class DeviceMessageSendTaskExecutorProvider implements TaskExecutorProvider {

    public static final String EXECUTOR = "device-message-sender";
    private final DeviceRegistry registry;

    private final DeviceSelectorBuilder selectorBuilder;

    @Override
    public String getExecutor() {
        return EXECUTOR;
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DeviceMessageSendTaskExecutor(context));
    }

    class DeviceMessageSendTaskExecutor extends FunctionTaskExecutor {

        private DeviceMessageSendConfig config;

        private Function<Map<String, Object>, Flux<DeviceOperator>> selector;

        public DeviceMessageSendTaskExecutor(ExecutionContext context) {
            super("发送设备消息", context);
            reload();
        }

        protected Flux<DeviceOperator> selectDevice(Map<String, Object> ctx) {
            return selector.apply(ctx);
        }

        @Override
        protected Publisher<RuleData> apply(RuleData input) {
            Map<String, Object> ctx = RuleDataHelper.toContextMap(input);

            Flux<DeviceOperator> readySendDevice =
                "ignoreOffline".equals(config.getStateOperator())
                    ? selectDevice(ctx).filterWhen(DeviceOperator::isOnline)
                    : selectDevice(ctx);

            return readySendDevice
                .switchIfEmpty(context.onError(() -> new DeviceOperationException(ErrorCode.SYSTEM_ERROR, "无可用设备"), input))
                .flatMap(device -> config
                    .doSend(ctx, context, device, input)
                    .onErrorResume(error -> context.onError(error, input))
                    .subscribeOn(Schedulers.parallel())
                )
                .map(reply -> {
                    RuleData data = context.newRuleData(input.newData(reply.toJson()));
                    if (config.getResponseHeaders() != null) {
                        config.getResponseHeaders().forEach(data::setHeader);
                    }
                    return data;
                })
                ;
        }

        @Override
        public void validate() {
            if (CollectionUtils.isEmpty(context.getJob().getConfiguration())) {
                throw new IllegalArgumentException("配置不能为空");
            }
            FastBeanCopier.copy(context.getJob().getConfiguration(), new DeviceMessageSendConfig()).validate();
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new DeviceMessageSendConfig());
            config.validate();
            if (config.getSelectorSpec() != null) {
                selector = selectorBuilder.createSelector(config.getSelectorSpec())::select;
            } else if (StringUtils.hasText(config.deviceId)) {
                selector = ctx -> registry.getDevice(config.getDeviceId()).flux();
            } else if (StringUtils.hasText(config.productId)) {
                selector = selectorBuilder.createSelector(DeviceSelectorProviders.product(config.productId))::select;
            } else {
                if (config.isFixed() && MapUtils.isNotEmpty(config.getMessage())) {
                    selector = ctx -> registry.getDevice(config.getDeviceIdInMessage(ctx)).flux();
                } else {
                    selector = ctx -> registry
                        .getDevice((String) ctx
                            .getOrDefault("deviceId",
                                          config.getMessage() == null
                                              ? null
                                              : config.getMessage().get("deviceId")))
                        .flux();
                }
            }
        }

    }


    @Getter
    @Setter
    public static class DeviceMessageSendConfig {

        //设备ID
        private String deviceId;

        //产品ID
        private String productId;

        //选择器描述
        private DeviceSelectorSpec selectorSpec;

        //消息来源: pre-node(上游节点),fixed(固定消息)
        private String from;

        private Duration timeout = Duration.ofSeconds(10);

        private Map<String, Object> message;

        private boolean async;

        private String waitType = "sync";

        private String stateOperator = "ignoreOffline";

        private DeviceSenderFlowLimitSpec deviceSenderFlowLimitSpec;

        //延迟执行
        private long delayMillis = 0;

        private Map<String, Object> responseHeaders;


        public Map<String, Object> toMap() {
            Map<String, Object> conf = FastBeanCopier.copy(this, new HashMap<>());
            conf.put("timeout", timeout.toString());
            return conf;
        }

        @SuppressWarnings("all")
        public Flux<DeviceMessage> doSend(Map<String, Object> ctx,
                                          ExecutionContext context,
                                          DeviceOperator device,
                                          RuleData input) {
            Map<String, Object> message = new HashMap<>("pre-node".equals(from) ? ctx : this.message);
            message.put("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
            message.put("deviceId", device.getDeviceId());
            message.put("timestamp", System.currentTimeMillis());
            return Mono
                .justOrEmpty(MessageType.convertMessage(message))
                .switchIfEmpty(context.onError(() -> new DeviceOperationException(ErrorCode.UNSUPPORTED_MESSAGE), input))
                .cast(DeviceMessage.class)
                .flatMap(msg -> applyMessageExpression(ctx, msg))
                .doOnNext(msg -> msg
                    .addHeader(Headers.async, async || !"sync".equals(waitType))
                    .addHeader(Headers.sendAndForget, "forget".equals(waitType))
                    .addHeader(Headers.timeout, timeout.toMillis()))
                .as(mono -> {
                    if (delayMillis > 0) {
                        return mono
                            .delayElement(Duration.ofMillis(delayMillis));
                    }
                    return mono;
                })
                .flatMapMany(msg -> splitMessageExpression(msg))
                .flatMap(msg -> "forget".equals(waitType)
                    ? device.messageSender().send(msg).then(Mono.empty())
                    : device.messageSender()
                            .send(msg)
                            .onErrorResume(err -> {
                                //失败尝试转为消息回复
                                if (msg instanceof RepayableDeviceMessage) {
                                    return Mono.just(((RepayableDeviceMessage<?>) msg).newReply().error(err));
                                }
                                return Mono.error(err);
                            })
                );
        }

        private Flux<? extends DeviceMessage> splitMessageExpression(DeviceMessage message) {
            if (Objects.isNull(deviceSenderFlowLimitSpec)) {
                return Flux.just(message);
            }
            if (message instanceof ReadPropertyMessage) {
                return splitMessageExpression((ReadPropertyMessage) message);
            }
            if (message instanceof WritePropertyMessage) {
                return splitMessageExpression((WritePropertyMessage) message);
            }
            return Flux.just(message);
        }

        private Flux<ReadPropertyMessage> splitMessageExpression(ReadPropertyMessage message) {
            List<String> properties = message.getProperties();
            return Flux
                .fromIterable(partition(properties, deviceSenderFlowLimitSpec.getCount()))
                .delayElements(Duration.ofMillis(deviceSenderFlowLimitSpec.getExecuteIntervalMillis(properties.size())))
                .map(p -> {
                    ReadPropertyMessage copy = FastBeanCopier.copy(message, new ReadPropertyMessage());
                    copy.setProperties(p);
                    copy.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
                    copy.setTimestamp(System.currentTimeMillis());
                    return copy;
                });
        }

        private Flux<WritePropertyMessage> splitMessageExpression(WritePropertyMessage message) {
            Map<String, Object> properties = message.getProperties();
            return Flux
                .fromIterable(partition(properties, deviceSenderFlowLimitSpec.getCount()))
                .delayElements(Duration.ofMillis(deviceSenderFlowLimitSpec.getExecuteIntervalMillis(properties.size())))
                .map(p -> {
                    WritePropertyMessage copy = FastBeanCopier.copy(message, new WritePropertyMessage());
                    copy.setProperties(p);
                    copy.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
                    copy.setTimestamp(System.currentTimeMillis());
                    return copy;
                });
        }

        private Mono<ReadPropertyMessage> applyMessageExpression(Map<String, Object> ctx, ReadPropertyMessage message) {
            List<String> properties = message.getProperties();
            if (!CollectionUtils.isEmpty(properties)) {
                message.setProperties(ConverterUtils.convertToList(message.getProperties(), prop -> (String) applyValueExpression(prop, ctx)));
            }

            return Mono.just(message);
        }

        private Mono<WritePropertyMessage> applyMessageExpression(Map<String, Object> ctx, WritePropertyMessage message) {
            Map<String, Object> properties = message.getProperties();

            if (!CollectionUtils.isEmpty(properties)) {
                message.setProperties(
                    Maps.transformValues(properties, v -> {
                        Object value = applyValueExpression(v, ctx);
                        return VariableSource.of(value).resolveStatic(ctx);
                    })
                );
            }

            return Mono.just(message);
        }

        private Mono<FunctionInvokeMessage> applyMessageExpression(Map<String, Object> ctx, FunctionInvokeMessage message) {
            List<FunctionParameter> inputs = message.getInputs();
            if (!CollectionUtils.isEmpty(inputs)) {
                for (FunctionParameter input : inputs) {
                    Object value = input.getValue();
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof String) {
                        input.setValue(applyValueExpression(value, ctx));
                    } else if (value instanceof List) {
                        input.setValue(ConverterUtils.convertToList(value, (v) -> VariableSource
                            .of(v)
                            .resolveStatic(ctx)));
                    } else {
                        input.setValue(VariableSource.of(value).resolveStatic(ctx));
                    }
                }
            }

            return Mono.just(message);
        }

        private String getDeviceIdInMessage(Map<String, Object> ctx) {
            String deviceId = (String) message.get("deviceId");

            if (StringUtils.hasText(deviceId)) {
                return String.valueOf(applyValueExpression(deviceId, ctx));
            }
            return null;
        }

        private Mono<? extends DeviceMessage> applyMessageExpression(Map<String, Object> ctx, DeviceMessage message) {
            if (message instanceof ReadPropertyMessage) {
                return applyMessageExpression(ctx, ((ReadPropertyMessage) message));
            }
            if (message instanceof WritePropertyMessage) {
                return applyMessageExpression(ctx, ((WritePropertyMessage) message));
            }
            if (message instanceof FunctionInvokeMessage) {
                return applyMessageExpression(ctx, ((FunctionInvokeMessage) message));
            }
            return Mono.just(message);
        }

        private boolean isFixed() {
            return "fixed".equals(from);
        }

        private boolean isPreNode() {
            return "pre-node".equals(from);
        }

        public void validate() {
            if ("fixed".equals(from)) {
                MessageType.convertMessage(message).orElseThrow(() -> new IllegalArgumentException("不支持的消息格式"));
            }
        }

        private Object applyValueExpression(Object value,
                                            Map<String, Object> ctx) {
            if (value instanceof String) {
                String stringValue = String.valueOf(value);
                if (stringValue.startsWith("${") && stringValue.endsWith("}"))
                    return ctx.get(stringValue.substring(2, stringValue.length() - 1));
            }
            return value;
        }

        private <T> List<List<T>> partition(List<T> list, int groupSize) {
            List<List<T>> partitions = new ArrayList<>();
            List<T> currentPartition = new ArrayList<>(groupSize);

            for (T item : list) {
                currentPartition.add(item);
                if (currentPartition.size() == groupSize) {
                    partitions.add(currentPartition);
                    currentPartition = new ArrayList<>(groupSize);
                }
            }

            // 添加最后一个可能不满的分区
            if (!currentPartition.isEmpty()) {
                partitions.add(currentPartition);
            }

            return partitions;
        }

        // 泛型方法，接受 Map<K, V> 类型的集合和一个整数作为分组大小
        private static <K, V> List<Map<K, V>> partition(Map<K, V> map, int groupSize) {

            List<Map.Entry<K, V>> entries = new ArrayList<>(map.entrySet());

            int partitionsSize = (int) Math.ceil((double) entries.size() / groupSize);

            List<Map<K, V>> partitions = new ArrayList<>(partitionsSize);

            for (int i = 0; i < partitionsSize; i++) {
                int fromIndex = i * groupSize;
                int toIndex = Math.min(fromIndex + groupSize, entries.size());
                Map<K, V> subMap = new HashMap<>(toIndex - fromIndex);
                List<Map.Entry<K, V>> partitionEntries = entries.subList(fromIndex, toIndex);

                for (Map.Entry<K, V> entry : partitionEntries) {
                    subMap.put(entry.getKey(), entry.getValue());
                }

                partitions.add(subMap);
            }
            return partitions;
        }
    }
}
