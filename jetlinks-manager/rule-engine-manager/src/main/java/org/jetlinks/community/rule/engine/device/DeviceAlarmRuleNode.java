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
import org.jetlinks.rule.engine.api.events.RuleEvent;
import org.jetlinks.rule.engine.api.executor.ExecutionContext;
import org.jetlinks.rule.engine.api.model.NodeType;
import org.jetlinks.rule.engine.executor.CommonExecutableRuleNodeFactoryStrategy;
import org.jetlinks.rule.engine.executor.node.RuleNodeConfig;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j(topic = "system.rule.engine.device.alarm")
@Component
@AllArgsConstructor
public class DeviceAlarmRuleNode extends CommonExecutableRuleNodeFactoryStrategy<DeviceAlarmRuleNode.Config> {

    private final MessageGateway messageGateway;

    @Override
    public Function<RuleData, ? extends Publisher<?>> createExecutor(ExecutionContext context,DeviceAlarmRuleNode.Config config) {

        return Mono::just;
    }

    @Override
    protected void onStarted(ExecutionContext context, DeviceAlarmRuleNode.Config config) {
        context.onStop(
            config.doSubscribe(messageGateway)
                .flatMap(result -> {
                    RuleData data = RuleData.create(result);
                    //输出到下一节点
                    return context.getOutput()
                        .write(Mono.just(data))
                        .then(context.fireEvent(RuleEvent.NODE_EXECUTE_DONE, data));
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
            "timestamp", "deviceId", "this.header.deviceName deviceName"
        );

        private DeviceAlarmRule rule;

        private ReactorQL ql;

        @Override
        public void validate() {
            if (CollectionUtils.isEmpty(rule.getTriggers())) {
                throw new IllegalArgumentException("预警条件不能为空");
            }
            try {
                ql = createQL();
            } catch (Exception e) {
                throw new IllegalArgumentException("配置错误:" + e.getMessage(), e);
            }
        }

        private ReactorQL createQL() {
            List<String> columns = new ArrayList<>(default_columns);
            List<String> wheres = new ArrayList<>();

            List<DeviceAlarmRule.Trigger> triggers = rule.getTriggers();

            for (int i = 0; i < triggers.size(); i++) {
                DeviceAlarmRule.Trigger trigger = triggers.get(i);
                // select this.properties.this trigger0
                columns.add(trigger.getType().getPropertyPrefix() + "this trigger" + i);
                columns.addAll(trigger.toColumns());
                trigger.createExpression()
                    .ifPresent(expr -> wheres.add("(" + expr + ")"));
            }
            String sql = "select \n\t\t" + String.join("\n\t\t,", columns) + " \n\tfrom dual ";

            if (!wheres.isEmpty()) {
                sql += "\n\twhere " + String.join("\n\t\t or ", wheres);
            }

            if (CollectionUtils.isNotEmpty(rule.getProperties())) {
                List<String> newColumns = new ArrayList<>(Arrays.asList(
                    "this.deviceName deviceName",
                    "this.deviceId deviceId",
                    "this.timestamp timestamp"));
                for (DeviceAlarmRule.Property property : rule.getProperties()) {
                    if (StringUtils.isEmpty(property.getProperty())) {
                        continue;
                    }
                    String alias = StringUtils.hasText(property.getAlias()) ? property.getAlias() : property.getProperty();
                    newColumns.add("this['" + property.getProperty() + "'] \"" + alias + "\"");
                }
                if (newColumns.size() > 3) {
                    sql = "select \n\t" + String.join("\n\t,", newColumns) + "\n from (\n\t" + sql + "\n) t";
                }
            }
            log.debug("create device alarm sql : \n{}", sql);
            return ReactorQL.builder().sql(sql).build();
        }

        public Flux<Map<String, Object>> doSubscribe(MessageGateway gateway) {
            Set<String> topics = new HashSet<>();

            List<Object> binds = new ArrayList<>();

            for (DeviceAlarmRule.Trigger trigger : rule.getTriggers()) {
                String topic = trigger.getType().getTopic(rule.getProductId(), rule.getDeviceId(), trigger.getModelId());
                topics.add(topic);
                binds.addAll(trigger.toFilterBinds());
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

            Flux<Map<String, Object>> resultFlux = (ql == null ? ql = createQL() : ql)
                .start(context)
                .map(ReactorQLRecord::asMap);

            DeviceAlarmRule.ShakeLimit shakeLimit;
            if ((shakeLimit = rule.getShakeLimit()) != null
                && shakeLimit.isEnabled()
                && shakeLimit.getTime() > 0) {
                int thresholdNumber = shakeLimit.getThreshold();
                //打开时间窗口
                Flux<Flux<Map<String, Object>>> window = resultFlux.window(Duration.ofSeconds(shakeLimit.getTime()));

                Function<Flux<Tuple2<Long, Map<String, Object>>>, Publisher<Tuple2<Long, Map<String, Object>>>> mapper =
                    shakeLimit.isAlarmFirst()
                        ?
                        group -> group
                            .takeUntil(tp -> tp.getT1() >= thresholdNumber) //达到触发阈值
                            .take(1) //取第一个
                            .singleOrEmpty()
                        :
                        group -> group.takeLast(1).singleOrEmpty();//取最后一个

                resultFlux = window
                    .flatMap(group -> group
                        .index((index, data) -> Tuples.of(index + 1, data))
                        .transform(mapper)
                        .filter(tp -> tp.getT1() >= thresholdNumber) //超过阈值告警
                        .map(tp2 -> {
                            tp2.getT2().put("totalAlarms", tp2.getT1());
                            return tp2.getT2();
                        }));
            }

            return resultFlux
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
                        log.debug("发生设备告警:{}", map);
                    }
                    // 推送告警信息到消息网关中
                    // /rule-engine/device/alarm/{productId}/{deviceId}/{ruleId}
                    return gateway
                        .publish(String.format(
                            "/rule-engine/device/alarm/%s/%s/%s",
                            rule.getProductId(), map.get("deviceId"), rule.getId()
                        ), map, true)
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
