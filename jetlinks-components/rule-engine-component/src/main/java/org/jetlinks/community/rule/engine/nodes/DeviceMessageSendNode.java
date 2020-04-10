package org.jetlinks.community.rule.engine.nodes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.Headers;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@AllArgsConstructor
@Component
public class DeviceMessageSendNode extends CommonExecutableRuleNodeFactoryStrategy<DeviceMessageSendNode.Config> {

    private final DeviceRegistry registry;

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, Config config) {

        return data -> {
            Flux<DeviceOperator> devices = StringUtils.hasText(config.getDeviceId())
                ? registry.getDevice(config.getDeviceId()).flux()
                : registry.getProduct(config.getProductId()).flatMapMany(DeviceProductOperator::getDevices);

            return devices
                .filterWhen(DeviceOperator::isOnline)
                .publishOn(Schedulers.parallel())
                .flatMap(config::doSend)
                .onErrorResume(error -> context.onError(data, error).then(Mono.empty()));
        };
    }

    @Override
    public String getSupportType() {
        return "device-message-sender";
    }

    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {

        //设备ID
        private String deviceId;

        //产品ID
        private String productId;

        private Map<String, Object> message;

        private boolean async;

        public Publisher<?> doSend(DeviceOperator device) {
            Map<String, Object> message = new HashMap<>(this.message);
            message.put("messageId", IDGenerator.SNOW_FLAKE_STRING.generate());
            message.put("deviceId", device.getDeviceId());
            return Mono
                .justOrEmpty(MessageType.convertMessage(message))
                .cast(RepayableDeviceMessage.class)
                .doOnNext(msg -> msg.addHeader(Headers.async, async))
                .flatMapMany(msg -> device.messageSender().send(Mono.just(msg)));
        }

        @Override
        public void validate() {
            if (StringUtils.isEmpty(deviceId) && StringUtils.isEmpty(productId)) {
                throw new IllegalArgumentException("deviceId和productId不能同时为空");
            }
            MessageType.convertMessage(message).orElseThrow(() -> new IllegalArgumentException("不支持的消息格式"));
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.MAP;
        }

        @Override
        public void setNodeType(NodeType nodeType) {

        }
    }

}
