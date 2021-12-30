package org.jetlinks.community.rule.engine.device;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.ValueObject;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.Jsonable;
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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
@Component
public class DeviceAlarmTaskExecutorProvider implements TaskExecutorProvider {

    private final EventBus eventBus;

    private final Scheduler scheduler;

    @Override
    public String getExecutor() {
        return "device_alarm";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new DeviceAlarmTaskExecutor(context, eventBus, scheduler));
    }

    static class DeviceAlarmTaskExecutor extends AbstractTaskExecutor {

        List<String> default_columns = Arrays.asList(
            "this.timestamp timestamp",
            "this.deviceId deviceId",
            "this.headers headers",
            "this.headers.deviceName deviceName",
            "this.messageType messageType"
        );
        private final EventBus eventBus;

        private final Scheduler scheduler;

        private DeviceAlarmRule rule;

        private final Map<DeviceAlarmRule.Trigger, ReactorQL> triggerQL = new ConcurrentHashMap<>();

        DeviceAlarmTaskExecutor(ExecutionContext context,
                                EventBus eventBus,
                                Scheduler scheduler) {
            super(context);
            this.eventBus = eventBus;
            this.scheduler = scheduler;
            init();
        }


        @Override
        public String getName() {
            return "设备告警";
        }

        @Override
        protected Disposable doStart() {
            rule.validate();
            return doSubscribe(eventBus)
                .filter(ignore -> state == Task.State.running)
                .flatMap(result -> {
                    RuleData data = context.newRuleData(result);
                    //输出到下一节点
                    return context
                        .getOutput()
                        .write(Mono.just(data))
                        .then(context.fireEvent(RuleConstants.Event.result, data));
                })
                .onErrorResume(err -> context.onError(err, null))
                .subscribe();
        }

        void init() {
            rule = createRule();
            Map<DeviceAlarmRule.Trigger, ReactorQL> ql = createQL(rule);
            triggerQL.clear();
            triggerQL.putAll(ql);
        }

        @Override
        public void reload() {
            init();
            if (disposable != null) {
                disposable.dispose();
            }
            disposable = doStart();
        }

        @Nonnull
        private DeviceAlarmRule createRule() {
            DeviceAlarmRule rule = ValueObject
                .of(context.getJob().getConfiguration())
                .get("rule")
                .map(val -> FastBeanCopier.copy(val, new DeviceAlarmRule()))
                .orElseThrow(() -> new IllegalArgumentException("error.alarm_configuration_error"));
            rule.validate();
            return rule;
        }

        @Override
        public void validate() {
            try {
                createQL(createRule());
            } catch (Exception e) {
                throw new BusinessException("error.configuration_error", 500, e.getMessage(), e);
            }
        }

        private ReactorQL createQL(int index, DeviceAlarmRule.Trigger trigger, DeviceAlarmRule rule) {
            List<String> columns = new ArrayList<>(default_columns);
            List<String> wheres = new ArrayList<>();

            // select this.properties.this trigger0
            columns.add(trigger.getType().getPropertyPrefix() + "this trigger" + index);
            columns.addAll(trigger.toColumns());
            trigger.createExpression()
                   .ifPresent(expr -> wheres.add("(" + expr + ")"));

            String sql = "select \n\t\t" + String.join("\n\t\t,", columns) + " \n\tfrom dual ";

            if (!wheres.isEmpty()) {
                sql += "\n\twhere " + String.join("\n\t\t or ", wheres);
            }

            if (CollectionUtils.isNotEmpty(rule.getProperties())) {
                List<String> newColumns = new ArrayList<>(default_columns);
                for (DeviceAlarmRule.Property property : rule.getProperties()) {
                    if (StringUtils.isEmpty(property.getProperty())) {
                        continue;
                    }
                    String alias = StringUtils.hasText(property.getAlias()) ? property.getAlias() : property.getProperty();
                    // 'message',func(),this[name]
                    if ((property.getProperty().startsWith("'") && property.getProperty().endsWith("'"))
                        ||
                        property.getProperty().contains("(") || property.getProperty().contains("[")) {
                        newColumns.add(property.getProperty() + " \"" + alias + "\"");
                    } else {
                        newColumns.add("this['" + property.getProperty() + "'] \"" + alias + "\"");
                    }
                }
                if (newColumns.size() > default_columns.size()) {
                    sql = "select \n\t" + String.join("\n\t,", newColumns) + "\n from (\n\t" + sql + "\n) t";
                }
            }
            log.debug("create device alarm sql : \n{}", sql);

            return ReactorQL.builder().sql(sql).build();
        }


        private Map<DeviceAlarmRule.Trigger, ReactorQL> createQL(DeviceAlarmRule rule) {
            Map<DeviceAlarmRule.Trigger, ReactorQL> qlMap = new HashMap<>();
            int index = 0;
            for (DeviceAlarmRule.Trigger trigger : rule.getTriggers()) {
                qlMap.put(trigger, createQL(index++, trigger, rule));
            }
            return qlMap;
        }

        public Flux<Map<String, Object>> doSubscribe(EventBus eventBus) {


            List<Flux<? extends Map<String, Object>>> inputs = new ArrayList<>();
            int index = 0;
            for (DeviceAlarmRule.Trigger trigger : rule.getTriggers()) {

                ReactorQL ql = triggerQL.get(trigger);
                if (ql == null) {
                    continue;
                }
                Flux<? extends Map<String, Object>> datasource;
                //since 1.11 定时触发的不从eventBus订阅
                if (trigger.getTrigger() == DeviceAlarmRule.TriggerType.timer) {
                    //从上游获取输入进行处理(通常是定时触发发送指令后得到的回复)
                    datasource = context
                        .getInput()
                        .accept()
                        .flatMap(RuleData::dataToMap);
                }
                //从事件总线中订阅数据
                else {
                    String topic = trigger
                        .getType()
                        .getTopic(rule.getProductId(), rule.getDeviceId(), trigger.getModelId());

                    //从事件总线订阅数据进行处理
                    Subscription subscription = Subscription.of(
                        "device_alarm:" + rule.getId() + ":" + index++,
                        topic,
                        Subscription.Feature.local
                    );
                    datasource = eventBus
                        .subscribe(subscription, DeviceMessage.class)
                        .map(Jsonable::toJson)
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
                        });

                }

                ReactorQLContext qlContext = ReactorQLContext.ofDatasource((t) -> datasource);
                trigger.toFilterBinds().forEach(qlContext::bind);
                inputs.add(ql.start(qlContext).map(ReactorQLRecord::asMap));
            }

            Flux<Map<String, Object>> resultFlux = Flux.merge(inputs);

            ShakeLimit shakeLimit;
            if ((shakeLimit = rule.getShakeLimit()) != null) {

                resultFlux = shakeLimit.transfer(
                    resultFlux,
                    (duration, flux) ->
                        StringUtils.hasText(rule.getDeviceId())
                            //规则已经指定了固定的设备,直接开启时间窗口就行
                            ? flux.window(duration, scheduler)
                            //规则配置在设备产品上,则按设备ID分组后再开窗口
                            : flux
                            .groupBy(map -> String.valueOf(map.get("deviceId")), Integer.MAX_VALUE)
                            .flatMap(group -> group.window(duration, scheduler), Integer.MAX_VALUE),
                    (alarm, total) -> alarm.put("totalAlarms", total)
                );
            }

            return resultFlux
                .flatMap(map -> {
                    @SuppressWarnings("all")
                    Map<String, Object> headers = (Map<String, Object>) map.remove("headers");
                    map.put("productId", rule.getProductId());
                    map.put("alarmId", rule.getId());
                    map.put("alarmName", rule.getName());
                    if (null != rule.getLevel()) {
                        map.put("alarmLevel", rule.getLevel());
                    }
                    if (null != rule.getType()) {
                        map.put("alarmType", rule.getType());
                    }
                    if (StringUtils.hasText(rule.getDeviceName())) {
                        map.putIfAbsent("deviceName", rule.getDeviceName());
                    }
                    if (StringUtils.hasText(rule.getProductName())) {
                        map.putIfAbsent("productName", rule.getProductName());
                    }
                    if (StringUtils.hasText(rule.getDeviceId())) {
                        map.putIfAbsent("deviceId", rule.getDeviceId());
                    }
                    if (!map.containsKey("deviceName") && map.get("deviceId") != null) {
                        map.putIfAbsent("deviceName", map.get("deviceId"));
                    }
                    if (!map.containsKey("productName")) {
                        map.putIfAbsent("productName", rule.getProductId());
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("发生设备告警:{}", map);
                    }

                    //生成告警记录时生成ID，方便下游做处理。
                    map.putIfAbsent("id", IDGenerator.MD5.generate());
                    // 推送告警信息到消息网关中
                    // /rule-engine/device/alarm/{productId}/{deviceId}/{ruleId}
                    return eventBus
                        .publish(String.format(
                            "/rule-engine/device/alarm/%s/%s/%s",
                            rule.getProductId(),
                            map.get("deviceId"),
                            rule.getId()), map)
                        .thenReturn(map);
                });
        }
    }

}
