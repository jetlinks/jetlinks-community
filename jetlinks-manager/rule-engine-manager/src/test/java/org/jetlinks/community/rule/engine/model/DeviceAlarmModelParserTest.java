package org.jetlinks.community.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.jetlinks.community.rule.engine.device.DeviceAlarmRule;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.enums.AlarmState;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceAlarmModelParserTest {


    @Test
    void parse() {
        DeviceAlarmModelParser parser = new DeviceAlarmModelParser();
        DeviceAlarmEntity entity = new DeviceAlarmEntity();
        entity.setId("id");
        entity.setName("name");
        entity.setTargetId("targetId");
        entity.setTarget("target");
        entity.setCreateTime(new Date());
        DeviceAlarmRule alarmRule = new DeviceAlarmRule();
        List<DeviceAlarmRule.Trigger> triggers = new ArrayList<>();
        DeviceAlarmRule.Trigger trigger = new DeviceAlarmRule.Trigger();
        trigger.setModelId("test");
        trigger.setTrigger(DeviceAlarmRule.TriggerType.timer);
        trigger.setType(DeviceAlarmRule.MessageType.online);
        trigger.setCron("1 1 1 1 1 1");
        trigger.setParameters(new ArrayList<>());
        triggers.add(trigger);
        alarmRule.setTriggers(triggers);
        entity.setAlarmRule(alarmRule);
        entity.setState(AlarmState.running);
        entity.setCreateTime(new Date());
        String s = JSON.toJSONString(entity);
        Executable executable = ()->parser.parse(s);
        assertThrows(UnsupportedOperationException.class,executable);
        trigger.setType(DeviceAlarmRule.MessageType.function);
        List<Action> actions = new ArrayList<>();
        Action action = new Action();
        actions.add(action);
        Action action1 = new Action();
        action1.setExecutor("ex");
        action1.setConfiguration(new HashMap<>());
        actions.add(action1);
        alarmRule.setActions(actions);

        String s1 = JSON.toJSONString(entity);
        RuleModel parse = parser.parse(s1);
        assertNotNull(parse);

    }
}