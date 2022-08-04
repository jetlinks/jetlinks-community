package org.jetlinks.community.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.rule.engine.device.DeviceAlarmRule;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.executor.DeviceMessageSendTaskExecutorProvider;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Component
public class DeviceAlarmModelParser implements RuleModelParserStrategy {

    public static String format = "device_alarm";

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public RuleModel parse(String modelDefineString) {
        //模型就是DeviceAlarmEntity的json
        DeviceAlarmEntity rule = FastBeanCopier.copy(JSON.parseObject(modelDefineString), DeviceAlarmEntity::new);

        RuleModel model = new RuleModel();
        model.setId("device_alarm:".concat(rule.getId()));
        model.setName(rule.getName());

        DeviceAlarmRule alarmRule = rule.getAlarmRule();
        //验证规则
        alarmRule.validate();

        //告警条件节点
        RuleNodeModel conditionNode = new RuleNodeModel();
        conditionNode.setId("conditions");
        conditionNode.setName("告警条件");
        conditionNode.setExecutor("device_alarm");
        conditionNode.setConfiguration(Collections.singletonMap("rule", rule.getAlarmRule()));

        //处理定时触发(定时向设备发送指令并获取返回结果)
        {
            List<DeviceAlarmRule.Trigger> timerTriggers = alarmRule
                .getTriggers()
                .stream()
                //定时节点
                .filter(trigger -> trigger.getTrigger() == DeviceAlarmRule.TriggerType.timer)
                .collect(Collectors.toList());
            int index = 0;
            for (DeviceAlarmRule.Trigger timerTrigger : timerTriggers) {
                DeviceMessage msg = timerTrigger.getType().createMessage(timerTrigger).orElse(null);
                if (msg == null) {
                    throw new BusinessException("error.unsupported_timing_condition_type", 500, timerTrigger.getType());
                }

                //定时节点
                //TimerTaskExecutorProvider
                RuleNodeModel timer = new RuleNodeModel();
                timer.setId("timer:" + (index));
                timer.setName("定时发送设备消息");
                timer.setExecutor("timer");
                timer.setConfiguration(Collections.singletonMap("cron", timerTrigger.getCron()));

                //发送指令节点
                //DeviceMessageSendTaskExecutorProvider
                DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig senderDeviceMessageSendConfig = new DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig();
                //同步等待回复
                senderDeviceMessageSendConfig.setAsync(false);
                //直接发送，不管设备是否在线
                senderDeviceMessageSendConfig.setStateOperator("direct");
                senderDeviceMessageSendConfig.setDeviceId(alarmRule.getDeviceId());
                senderDeviceMessageSendConfig.setProductId(alarmRule.getProductId());
                senderDeviceMessageSendConfig.setMessage(msg.toJson());
                // 添加自定义响应头到RuleData中
                // 用于在收到结果时,判断是由哪个触发条件触发的
                // 因为所有告警节点只有一个,所有的定时执行结果都会输入到同一个节点中
                senderDeviceMessageSendConfig.setResponseHeaders(Collections.singletonMap("triggerIndex", index));
                //设备指令发送节点
                //DeviceMessageSendTaskExecutorProvider
                RuleNodeModel messageSender = new RuleNodeModel();
                messageSender.setId("message-sender:" + (index));
                messageSender.setName("定时发送设备消息");
                messageSender.setExecutor("device-message-sender");
                messageSender.setConfiguration(senderDeviceMessageSendConfig.toMap());
                //连接定时和设备指令节点
                RuleLink link = new RuleLink();
                link.setId(timer.getId().concat(":").concat(messageSender.getId()));
                link.setName("发送指令:" + index);
                link.setSource(timer);
                link.setTarget(messageSender);
                //timer -> device-message-sender
                timer.getOutputs().add(link);
                //device-message-sender -> timer
                messageSender.getInputs().add(link);

                //添加定时和消息节点到模型
                model.getNodes().add(timer);
                model.getNodes().add(messageSender);

                //将设备指令和告警条件节点连接起来
                RuleLink toAlarm = new RuleLink();
                toAlarm.setId(messageSender.getId().concat(":").concat(conditionNode.getId()));
                toAlarm.setName("定时触发告警:" + index);
                toAlarm.setSource(messageSender);
                toAlarm.setTarget(conditionNode);
                messageSender.getOutputs().add(toAlarm);
                conditionNode.getInputs().add(toAlarm);
                index++;
            }
        }

        //添加告警条件到模型
        model.getNodes().add(conditionNode);
        //执行动作
        if (CollectionUtils.isNotEmpty(rule.getAlarmRule().getActions())) {
            int index = 0;
            for (Action operation : rule.getAlarmRule().getActions()) {
                if (!StringUtils.hasText(operation.getExecutor())) {
                    continue;
                }
                index++;
                RuleNodeModel action = new RuleNodeModel();
                action.setId("device_alarm_action:" + index);
                action.setName("执行动作:" + index);
                action.setExecutor(operation.getExecutor());
                action.setConfiguration(operation.getConfiguration());

                RuleLink link = new RuleLink();
                link.setId(action.getId().concat(":").concat(conditionNode.getId()));
                link.setName("执行动作:" + index);
                link.setSource(conditionNode);
                link.setTarget(action);
                model.getNodes().add(action);
                action.getInputs().add(link);
                conditionNode.getOutputs().add(link);
            }
        }
        return model;
    }
}
