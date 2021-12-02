package org.jetlinks.community.rule.engine.device;

import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.message.DeviceOnlineMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.Input;
import org.jetlinks.rule.engine.api.task.Output;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DeviceAlarmTaskExecutorProviderTest {
    private static final String ID = "test";
//    private final
    @Test
    void getExecutor() {
        DeviceAlarmTaskExecutorProvider provider = new DeviceAlarmTaskExecutorProvider(new BrokerEventBus(), Schedulers.parallel());
        String executor = provider.getExecutor();
        assertNotNull(executor);
    }

    @Test
    void createTask() {
        ExecutionContext context = Mockito.mock(ExecutionContext.class);

        DeviceAlarmTaskExecutorProvider provider = new DeviceAlarmTaskExecutorProvider(new BrokerEventBus(), Schedulers.parallel());
        ScheduleJob scheduleJob = new ScheduleJob();
        Map<String, Object> map = new HashMap<>();
        DeviceAlarmRule rule = new DeviceAlarmRule();
        rule.setId(ID);
        List<DeviceAlarmRule.Trigger> list = new ArrayList<>();
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        trigger.setCron("1 1 1 1 1 1");
        trigger.setModelId("test");
        trigger.setType(DeviceAlarmRule.MessageType.online);
        trigger.setTrigger(DeviceAlarmRule.TriggerType.timer);
        //设置trigger 的条件filters
        List<DeviceAlarmRule.ConditionFilter> filters = new ArrayList<>();
        DeviceAlarmRule.ConditionFilter filter = new DeviceAlarmRule.ConditionFilter();
        filter.setKey("key");
        filter.setValue("value");
        filters.add(filter);
        trigger.setFilters(filters);
        list.add(trigger);

        //设置rule 的properties自定义字段映射
        rule.setTriggers(list);
        List<DeviceAlarmRule.Property> properties = new ArrayList<>();
        DeviceAlarmRule.Property property1 = new DeviceAlarmRule.Property();
        property1.setProperty("property1");
        property1.setAlias("alias1");
        DeviceAlarmRule.Property property2 = new DeviceAlarmRule.Property();
        property2.setProperty("'property2'");
        DeviceAlarmRule.Property property3 = new DeviceAlarmRule.Property();
        property3.setProperty("(property3)");
        property3.setAlias("alias3");
        DeviceAlarmRule.Property property5 = new DeviceAlarmRule.Property();
        properties.add(property1);
        properties.add(property2);
        properties.add(property3);
        properties.add(property5);
        rule.setProperties(properties);
        map.put("rule", rule);
        scheduleJob.setConfiguration(map);
        Mockito.when(context.getJob())
            .thenReturn(scheduleJob);
        provider.createTask(context);
    }

    /*  DeviceAlarmTaskExecutor   */
    @Test
    void deviceAlarmTaskExecutor() {
        ExecutionContext context = Mockito.mock(ExecutionContext.class);
        EventBus eventBus = Mockito.mock(EventBus.class);
        DeviceAlarmTaskExecutorProvider provider = new DeviceAlarmTaskExecutorProvider(eventBus, Schedulers.parallel());

        ScheduleJob scheduleJob = new ScheduleJob();
        Map<String, Object> map = new HashMap<>();
        DeviceAlarmRule rule = new DeviceAlarmRule();
        rule.setId(ID);
        rule.setProductId("ProductId");
        rule.setProductName("ProductName");
        List<DeviceAlarmRule.Trigger> list = new ArrayList<>();
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        trigger.setCron("1 1 1 1 1 1");
        trigger.setModelId("test");
        trigger.setType(DeviceAlarmRule.MessageType.online);
        trigger.setTrigger(DeviceAlarmRule.TriggerType.timer);
        //设置trigger 的条件filters
        List<DeviceAlarmRule.ConditionFilter> filters = new ArrayList<>();
        DeviceAlarmRule.ConditionFilter filter = new DeviceAlarmRule.ConditionFilter();
        filter.setKey("key");
        filter.setValue("value");
        filters.add(filter);
        trigger.setFilters(filters);


        list.add(trigger);
        //设置rule 的properties自定义字段映射
        rule.setTriggers(list);
        List<DeviceAlarmRule.Property> properties = new ArrayList<>();
        DeviceAlarmRule.Property property1 = new DeviceAlarmRule.Property();
        property1.setProperty("deviceId");
        property1.setAlias("deviceId");
        DeviceAlarmRule.Property property2 = new DeviceAlarmRule.Property();
        property2.setProperty("'property2'");
        DeviceAlarmRule.Property property3 = new DeviceAlarmRule.Property();
        property3.setProperty("(property3)");
        property3.setAlias("alias3");
        DeviceAlarmRule.Property property5 = new DeviceAlarmRule.Property();
        properties.add(property1);
        properties.add(property2);
        properties.add(property3);
        properties.add(property5);
        rule.setProperties(properties);
        map.put("rule", rule);
        scheduleJob.setConfiguration(map);

        //设置rule的防抖限制shakeLimit
        ShakeLimit shakeLimit = new ShakeLimit();
        //关闭
        shakeLimit.setEnabled(false);
        shakeLimit.setTime(1);
        shakeLimit.setThreshold(1);
        shakeLimit.setAlarmFirst(false);
        rule.setShakeLimit(shakeLimit);

        Mockito.when(context.getJob())
            .thenReturn(scheduleJob);
        DeviceAlarmTaskExecutorProvider.DeviceAlarmTaskExecutor taskExecutor = (DeviceAlarmTaskExecutorProvider.DeviceAlarmTaskExecutor)provider.createTask(context).block();
        assertNotNull(taskExecutor);
        String name = taskExecutor.getName();
        assertNotNull(name);

        Input input = Mockito.mock(Input.class);
        Mockito.when(context.getInput())
            .thenReturn(input);
        RuleData ruleData = new RuleData();
        ruleData.setId("ruleDataId");
        ruleData.setContextId("test");
        Map<String, Object> map1 = new HashMap<>();
        map1.put("test","test");
        ruleData.setData(map1);
        ruleData.setHeaders(new HashMap<>());
        Mockito.when(input.accept())
            .thenReturn(Flux.just(ruleData));
        taskExecutor.reload();
        //开启抖动限制
        shakeLimit.setEnabled(true);
        taskExecutor.reload();
        taskExecutor.reload();

        rule.setDeviceId("DeviceID");
        rule.setDeviceName("DeviceName");

        DeviceAlarmRule.Trigger trigger1 = new DeviceAlarmRule.Trigger();
        trigger1.setCron("1 1 1 1 1 1");
        trigger1.setModelId("test");
        trigger1.setType(DeviceAlarmRule.MessageType.online);
        trigger1.setTrigger(DeviceAlarmRule.TriggerType.device);
        //设置trigger 的条件filters
        trigger.setFilters(filters);

        list.add(trigger1);
        DeviceOnlineMessage message = new DeviceOnlineMessage();
        Mockito.when(eventBus.subscribe(Mockito.any(Subscription.class),Mockito.any(Class.class)))
            .thenReturn(Flux.just(message));
        RuleData data = new RuleData();
        Mockito.when(context.newRuleData(Mockito.any(Object.class)))
            .thenReturn(data);
        Output output = Mockito.mock(Output.class);
        Mockito.when(context.getOutput())
            .thenReturn(output);
        Mockito.when(output.write(Mockito.any(Publisher.class)))
            .thenReturn(Mono.just(true));
        Mockito.when(context.fireEvent(Mockito.anyString(),Mockito.any(RuleData.class)))
            .thenReturn(Mono.just(1));
        taskExecutor.start();
        taskExecutor.reload();

        shakeLimit.setThreshold(-1);
        taskExecutor.reload();

        taskExecutor.validate();



//        Mockito.when(eventBus.publish(Mockito.anyString(),Mockito.any(Map.class)))
//            .thenReturn(Mono.just(1L));
//        taskExecutor.doSubscribe(eventBus)
//            .as(StepVerifier::create)
//            .expectSubscription()
//            .verifyComplete();

    }
}