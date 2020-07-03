package org.jetlinks.community.rule.engine.device;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.gateway.DeviceMessageUtils;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.rule.engine.api.RuleConstants;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Component
public class DeviceAlarmTaskExecutorProvider implements TaskExecutorProvider {

    private final MessageGateway messageGateway;

    @Override
    public String getExecutor() {
        return "device_alarm";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DeviceAlarmTaskExecutor(context));
    }

    class DeviceAlarmTaskExecutor extends AbstractTaskExecutor {

        List<String> default_columns = Arrays.asList(
            "timestamp", "deviceId", "this.header.deviceName deviceName"
        );

        private DeviceAlarmRule rule;

        private ReactorQL ql;

        public DeviceAlarmTaskExecutor(ExecutionContext context) {
            super(context);
            rule = createRule();
            ql = createQL(rule);
        }

        @Override
        public String getName() {
            return "设备告警";
        }

        @Override
        protected Disposable doStart() {
            rule.validate();
            return doSubscribe(messageGateway)
                .filter(ignore -> state == Task.State.running)
                .flatMap(result -> {
                    RuleData data = RuleData.create(result);
                    //输出到下一节点
                    return context
                        .getOutput()
                        .write(Mono.just(data))
                        .then(context.fireEvent(RuleConstants.Event.result, data));
                })
                .onErrorResume(err -> context.onError(err, null))
                .subscribe();
        }

        @Override
        public void reload() {
            rule = createRule();
            ql = createQL(rule);
            if (disposable != null) {
                disposable.dispose();
            }
            doStart();
        }

        private DeviceAlarmRule createRule() {
            DeviceAlarmRule rule = ValueObject.of(context.getJob().getConfiguration())
                .get("rule")
                .map(val -> FastBeanCopier.copy(val, new DeviceAlarmRule())).orElseThrow(() -> new IllegalArgumentException("告警配置错误"));
            rule.validate();
            return rule;
        }

        @Override
        public void validate() {
            DeviceAlarmRule rule = createRule();
            try {
                createQL(rule);
            } catch (Exception e) {
                throw new IllegalArgumentException("配置错误:" + e.getMessage(), e);
            }
        }

        private ReactorQL createQL(DeviceAlarmRule rule) {
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
                    // 'message',func(),this[name]
                    if ((property.getProperty().startsWith("'") && property.getProperty().endsWith("'"))
                        ||
                        property.getProperty().contains("(") || property.getProperty().contains("[")) {
                        newColumns.add(property.getProperty() + "\"" + alias + "\"");
                    } else {
                        newColumns.add("this['" + property.getProperty() + "'] \"" + alias + "\"");
                    }
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

            Flux<Map<String, Object>> resultFlux = (ql == null ? ql = createQL(rule) : ql)
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
                            .takeUntil(tp -> tp.getT1() >= thresholdNumber)
                            .take(1)
                            .singleOrEmpty()
                        :
                        group -> group.takeLast(1).singleOrEmpty();

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
    }
}
