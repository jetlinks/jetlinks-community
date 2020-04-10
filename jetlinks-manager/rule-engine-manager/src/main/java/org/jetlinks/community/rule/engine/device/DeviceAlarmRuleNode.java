package org.jetlinks.community.rule.engine.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLRecord;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j(topic = "system.rule.engine.device.alarm")
@Component
@AllArgsConstructor
public class DeviceAlarmRuleNode extends CommonExecutableRuleNodeFactoryStrategy<DeviceAlarmRuleNode.Config> {

    private final MessageGateway messageGateway;

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context, org.jetlinks.community.rule.engine.device.DeviceAlarmRuleNode.Config config) {

        return Mono::just;
    }

    @Override
    protected void onStarted(ExecutionContext context, org.jetlinks.community.rule.engine.device.DeviceAlarmRuleNode.Config config) {
        context.onStop(
            config.doSubscribe(messageGateway)
                .flatMap(result -> {
                    //输出到下一节点
                    return context.getOutput()
                        .write(Mono.just(RuleData.create(result)))
                        .then();
                })
                .onErrorResume(err -> context.onError(RuleData.create(err.getMessage()), err))
                .subscribe()::dispose
        );
    }

    @Override
    public String getSupportType() {
        return "device_alarm";
    }


    @Getter
    @Setter
    public static class Config implements RuleNodeConfig {
        static List<String> default_columns = Arrays.asList(
            "timestamp", "deviceId"
        );

        private DeviceAlarmRule rule;

        @Override
        public void validate() {
            if (CollectionUtils.isEmpty(rule.getConditions())) {
                throw new IllegalArgumentException("预警条件不能为空");
            }
        }

        private ReactorQL createQL() {
            List<String> columns = new ArrayList<>(default_columns);
            List<String> wheres = new ArrayList<>();
            columns.addAll(rule.getPlainColumns());

            for (DeviceAlarmRule.Condition condition : rule.getConditions()) {
                wheres.add(condition.createExpression(rule.getType()));
            }

            String sql = "select " + String.join(",", columns) +
                " from msg where " + String.join(" or ", wheres);

            log.debug("create device alarm sql : {}", sql);
            return ReactorQL.builder().sql(sql).build();
        }

        public Flux<Map<String, Object>> doSubscribe(MessageGateway gateway) {
            Set<String> topics = new HashSet<>();

            List<Object> binds = new ArrayList<>();

            for (DeviceAlarmRule.Condition condition : rule.getConditions()) {
                String topic = rule.getType().getTopic(rule.getProductId(), rule.getDeviceId(), condition.getModelId());
                topics.add(topic);
                binds.add(condition.convertValue());
            }
            List<Subscription> subscriptions = topics.stream().map(Subscription::new).collect(Collectors.toList());

            ReactorQLContext context = ReactorQLContext
                .ofDatasource(ignore ->
                    gateway
                        .subscribe(subscriptions, "device_alarm:" + rule.getId(), false)
                        .flatMap(msg -> Mono.justOrEmpty(DeviceMessageUtils.convert(msg).map(DeviceMessage::toJson)))
                        .doOnNext(json -> {
                            if (StringUtils.hasText(rule.getDeviceName())) {
                                json.putIfAbsent("deviceName", rule.getDeviceName());
                            }
                            if (StringUtils.hasText(rule.getProductName())) {
                                json.putIfAbsent("productName", rule.getProductName());
                            }
                            json.put("productId", rule.getProductId());
                            json.put("alarmId", rule.getId());
                            json.put("alarmName", rule.getName());
                        })
                );

            binds.forEach(context::bind);
            return createQL()
                .start(context)
                .map(ReactorQLRecord::asMap)
                .flatMap(map -> {
                    map.put("productId", rule.getProductId());
                    map.put("alarmId", rule.getId());
                    map.put("alarmName", rule.getName());
                    if (StringUtils.hasText(rule.getDeviceName())) {
                        map.putIfAbsent("deviceName", rule.getDeviceName());
                    }
                    if (StringUtils.hasText(rule.getProductName())) {
                        map.putIfAbsent("productName", rule.getProductName());
                    }
                    if (StringUtils.hasText(rule.getDeviceId())) {
                        map.putIfAbsent("deviceId", rule.getDeviceId());
                    }
                    if (!map.containsKey("deviceName")) {
                        map.putIfAbsent("deviceName", map.get("deviceId"));
                    }
                    if (!map.containsKey("productName")) {
                        map.putIfAbsent("productName", map.get("productId"));
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("发生设备预警:{}", map);
                    }
                    // 推送警告到消息网关中
                    // /rule-engine/device/alarm/{productId}/{deviceId}/{ruleId}
                    return gateway
                        .publish(String.format(
                            "/rule-engine/device/alarm/%s/%s/%s",
                            rule.getProductId(), map.get("deviceId"), rule.getId()
                        ), map)
                        .then(Mono.just(map));
                });
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
