package org.jetlinks.community.rule.engine.device;

import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DeviceAlarmRuleTest {

    @Test
    void validate() {
        DeviceAlarmRule rule = new DeviceAlarmRule();
        List<DeviceAlarmRule.Trigger> list = new ArrayList<>();
        rule.setTriggers(list);
        Executable executable = ()->rule.validate();
        assertThrows(IllegalArgumentException.class,executable,"触发条件不能为空");
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        trigger.setType(DeviceAlarmRule.MessageType.event);
        trigger.setModelId("modelId");
        trigger.setTrigger(DeviceAlarmRule.TriggerType.timer);
        trigger.setCron("1 1 1 1 1 1");
        list.add(trigger);
        rule.validate();
    }

/**   Trigger  **/
    @Test
    void toColumns(){
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        trigger.setFilters(new ArrayList());
        Set<String> strings1 = trigger.toColumns();
        assertNotNull(strings1);
        trigger.setModelId("test");
        trigger.setType(DeviceAlarmRule.MessageType.online);
        List<DeviceAlarmRule.ConditionFilter> filters = new ArrayList<>();
        DeviceAlarmRule.ConditionFilter filter = new DeviceAlarmRule.ConditionFilter();
        filter.setKey("test");
        filters.add(filter);
        trigger.setFilters(filters);
        Set<String> strings = trigger.toColumns();
        assertNotNull(strings);


    }
    @Test
    void toFilterBinds(){
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        trigger.toFilterBinds();
        List<DeviceAlarmRule.ConditionFilter> filters = new ArrayList<>();
        DeviceAlarmRule.ConditionFilter filter = new DeviceAlarmRule.ConditionFilter();
        filter.setKey("test");
        filters.add(filter);
        trigger.setFilters(filters);
        List<Object> objects = trigger.toFilterBinds();
        assertNotNull(objects);
    }
    @Test
    void createExpression(){
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        trigger.setFilters(new ArrayList<>());
        String s1 = trigger.createExpression().orElse("a");
        assertNotNull(s1);

        trigger.setType(DeviceAlarmRule.MessageType.online);
        List<DeviceAlarmRule.ConditionFilter> filters = new ArrayList<>();
        DeviceAlarmRule.ConditionFilter filter = new DeviceAlarmRule.ConditionFilter();
        filter.setKey("(test");
        filters.add(filter);
        trigger.setFilters(filters);
        String s = trigger.createExpression().orElse("a");
        assertNotNull(s);

    }
    @Test
    void validateTrigger(){
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        Executable executable = ()->trigger.validate();
        assertThrows(IllegalArgumentException.class,executable,"类型不能为空");

        trigger.setType(DeviceAlarmRule.MessageType.event);
        Executable executable1 = ()->trigger.validate();
        assertThrows(IllegalArgumentException.class,executable1,"属性/事件/功能ID不能为空");

        trigger.setTrigger(DeviceAlarmRule.TriggerType.timer);
        trigger.setModelId("test");
        Executable executable2 = ()->trigger.validate();
        assertThrows(IllegalArgumentException.class,executable2,"cron表达式不能为空");


        trigger.setCron("s");
        Executable executable3 = ()->trigger.validate();
        assertThrows(IllegalArgumentException.class,executable3,"cron表达式格式错误");

        trigger.setCron("1 1 1 1 1 1");
        List<DeviceAlarmRule.ConditionFilter> filters = new ArrayList();
        DeviceAlarmRule.ConditionFilter filter = new DeviceAlarmRule.ConditionFilter();
        filter.setKey("(test");
        filter.setValue("(test");
        filters.add(filter);
        trigger.setFilters(filters);
        trigger.validate();

        List<FunctionParameter> parameters = new ArrayList<>();
        trigger.setParameters(parameters);
        assertNotNull(trigger.getCron());
        assertNotNull(trigger.getFilters());
        assertNotNull(trigger.getModelId());
        assertNotNull(trigger.getParameters());
        assertNotNull(trigger.getTrigger());
        assertNotNull(trigger.getType());


    }

/**   TriggerType  **/
    @Test
    void get(){

        assertNotNull(DeviceAlarmRule.TriggerType.device.getSupportMessageTypes());
    }

/**   MessageType  **/
    @Test
    void on(){
        String topicOnline = DeviceAlarmRule.MessageType.online.getTopic("productId", "deviceId", "key");
        assertNotNull(topicOnline);
        String topicOffline = DeviceAlarmRule.MessageType.offline.getTopic("productId", "deviceId", "key");
        assertNotNull(topicOffline);
        String topicProperties = DeviceAlarmRule.MessageType.properties.getTopic("productId", "deviceId", "key");
        assertNotNull(topicProperties);
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        DeviceMessage deviceMessage = DeviceAlarmRule.MessageType.properties.createMessage(trigger).get();
        assertNotNull(deviceMessage);
        trigger.setModelId("test");
        DeviceMessage deviceMessage1 = DeviceAlarmRule.MessageType.properties.createMessage(trigger).get();
        assertNotNull(deviceMessage1);

        String topicEvent = DeviceAlarmRule.MessageType.event.getTopic("productId", "deviceId", "property");
        assertNotNull(topicEvent);

        String topicFunction = DeviceAlarmRule.MessageType.function.getTopic("productId", "deviceId", "property");
        assertNotNull(topicFunction);
        trigger.setParameters(new ArrayList<>());
        DeviceMessage message = DeviceAlarmRule.MessageType.function.createMessage(trigger).get();
        assertNotNull(message);
        DeviceAlarmRule.MessageType.online.createMessage(trigger);

    }
/**   ConditionFilter  **/
    @Test
    void conditionFilter(){
        DeviceAlarmRule.ConditionFilter filter = new DeviceAlarmRule.ConditionFilter();
        Executable executable=()->filter.validate();
        assertThrows(IllegalArgumentException.class,executable,"条件key不能为空");
        filter.setKey("test");
        Executable executable1=()->filter.validate();
        assertThrows(IllegalArgumentException.class,executable1,"条件值不能为空");
        filter.setValue("test");



        String expression = filter.createExpression(DeviceAlarmRule.MessageType.event, true);
        assertNotNull(expression);
        String expression1 = filter.createExpression(DeviceAlarmRule.MessageType.event, false);
        assertNotNull(expression1);

        boolean b = filter.valueIsExpression();
        assertFalse(b);

        assertNotNull(filter.getKey());
        assertNotNull(filter.getValue());
        assertNotNull(filter.getOperator());
    }

/**   Property  **/
    @Test
    void property(){
        DeviceAlarmRule.Property property = new DeviceAlarmRule.Property();
        property.setAlias("alias");
        property.setProperty("property");
        assertNotNull(property.getAlias());
        assertNotNull(property.getProperty());
        property.toString();
        String symbol = DeviceAlarmRule.Operator.eq.getSymbol();
        assertNotNull(symbol);
    }
}