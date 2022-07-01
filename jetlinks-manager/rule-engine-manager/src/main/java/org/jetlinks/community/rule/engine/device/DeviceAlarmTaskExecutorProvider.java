package org.jetlinks.community.rule.engine.device;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.ValueObject;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.metadata.Jsonable;
import org.jetlinks.core.utils.FluxUtils;
import org.jetlinks.reactor.ql.ReactorQL;
import org.jetlinks.reactor.ql.ReactorQLContext;
import org.jetlinks.reactor.ql.ReactorQLRecord;
import org.jetlinks.reactor.ql.utils.CastUtils;
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

        /**
         * 默认要查询的列
         */
        static List<String> default_columns = Arrays.asList(
            //时间戳
            "this.timestamp timestamp",
            //设备ID
            "this.deviceId deviceId",
            //header
            "this.headers headers",
            //设备名称,通过DeviceMessageConnector自定填充了值
            "this.headers.deviceName deviceName",
            //消息唯一ID
            "this.headers._uid _uid",
            //消息类型,下游可以根据消息类型来做处理,比如:离线时,如果网关设备也不在线则不触发.
            "this.messageType messageType"
        );
        private final EventBus eventBus;

        private final Scheduler scheduler;

        //触发器对应的ReactorQL缓存
        private final Map<DeviceAlarmRule.Trigger, ReactorQL> triggerQL = new ConcurrentHashMap<>();

        //告警规则
        private DeviceAlarmRule rule;

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

        static ReactorQL createQL(int index, DeviceAlarmRule.Trigger trigger, DeviceAlarmRule rule) {
            String sql = trigger.toSQL(index, default_columns, rule.getProperties());
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

            //满足触发条件的输出数据流
            List<Flux<? extends Map<String, Object>>> triggerOutputs = new ArrayList<>();

            int index = 0;

            //上游节点的输入
            //定时触发时: 定时节点输出到设备指令节点,设备指令节点输出到当前节点
            Flux<RuleData> input = context
                .getInput()
                .accept()
                //使用cache,多个定时收到相同的数据
                //通过header来进行判断具体是哪个触发器触发的,应该还有更好的方式.
                .replay(0)
                .refCount(1,Duration.ofMillis(10));

            for (DeviceAlarmRule.Trigger trigger : rule.getTriggers()) {
                //QL不存在,理论上不会发生
                ReactorQL ql = triggerQL.get(trigger);
                if (ql == null) {
                    log.warn("DeviceAlarmRule trigger {} init error", index);
                    continue;
                }
                Flux<? extends Map<String, Object>> datasource;

                int currentIndex = index;
                //since 1.11 定时触发的不从eventBus订阅
                if (trigger.getTrigger() == DeviceAlarmRule.TriggerType.timer) {
                    //从上游获取输入进行处理(通常是定时触发发送指令后得到的回复)
                    datasource = input
                        .filter(data -> {
                            //通过上游输出的header来判断是否为同一个触发规则，还有更好的方式?
                            return data
                                .getHeader("triggerIndex")
                                .map(idx -> CastUtils.castNumber(idx).intValue() == currentIndex)
                                .orElse(true);
                        })
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
                        .map(Jsonable::toJson);

                }

                ReactorQLContext qlContext = ReactorQLContext
                    .ofDatasource((t) -> datasource
                        .doOnNext(map -> {
                            if (StringUtils.hasText(rule.getDeviceName())) {
                                map.putIfAbsent("deviceName", rule.getDeviceName());
                            }
                            if (StringUtils.hasText(rule.getProductName())) {
                                map.putIfAbsent("productName", rule.getProductName());
                            }
                            map.put("productId", rule.getProductId());
                            map.put("alarmId", rule.getId());
                            map.put("alarmName", rule.getName());
                        }));
                //绑定SQL中的预编译变量
                trigger.toFilterBinds().forEach(qlContext::bind);

                //启动ReactorQL进行实时数据处理
                triggerOutputs.add(ql.start(qlContext).map(ReactorQLRecord::asMap));
            }

            Flux<Map<String, Object>> resultFlux = Flux.merge(triggerOutputs);

            //防抖
            ShakeLimit shakeLimit;
            if ((shakeLimit = rule.getShakeLimit()) != null) {

                resultFlux = shakeLimit.transfer(
                    resultFlux,
                    (duration, flux) ->
                        StringUtils.hasText(rule.getDeviceId())
                            //规则已经指定了固定的设备,直接开启时间窗口就行
                            ? flux.window(duration, scheduler)
                            //规则配置在设备产品上,则按设备ID分组后再开窗口
                            //设备越多,消耗的内存越大
                            : flux
                            .groupBy(map -> String.valueOf(map.get("deviceId")), Integer.MAX_VALUE)
                            .flatMap(group -> group.window(duration, scheduler), Integer.MAX_VALUE),
                    (alarm, total) -> alarm.put("totalAlarms", total)
                );
            }

            return resultFlux
                .as(result -> {
                    //有多个触发条件时对重复的数据进行去重,
                    //防止同时满足条件时会产生多个告警记录
                    if (rule.getTriggers().size() > 1) {
                        return result
                            .as(FluxUtils.distinct(
                                map -> map.getOrDefault(PropertyConstants.uid.getKey(), ""),
                                Duration.ofSeconds(1)));
                    }
                    return result;
                })
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
