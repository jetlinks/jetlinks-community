package org.jetlinks.community.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.community.rule.engine.device.DeviceAlarmRule;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.community.rule.engine.nodes.DeviceMessageSendNode;
import org.jetlinks.rule.engine.api.cluster.RunMode;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
        DeviceAlarmEntity rule = FastBeanCopier.copy(JSON.parseObject(modelDefineString), DeviceAlarmEntity::new);

        RuleModel model = new RuleModel();
        model.setId("device_alarm:".concat(rule.getId()));
        model.setName(rule.getName());
        model.setRunMode(RunMode.CLUSTER);

        DeviceAlarmRule alarmRule = rule.getAlarmRule();
        alarmRule.validate();

        //处理定时触发
        {
            List<DeviceAlarmRule.Condition> timerConditions = alarmRule.getConditions().stream()
                .filter(condition -> condition.getTrigger() == DeviceAlarmRule.ConditionType.timer)
                .collect(Collectors.toList());
            int index = 0;
            for (DeviceAlarmRule.Condition timerCondition : timerConditions) {
                DeviceMessage msg = alarmRule.getType().createMessage(timerCondition).orElse(null);
                if (msg == null) {
                    throw new UnsupportedOperationException("不支持定时条件类型:" + alarmRule.getType());
                }
                RuleNodeModel timer = new RuleNodeModel();
                timer.setId("timer:" + (++index));
                timer.setName("定时发送设备消息");
                timer.setExecutor("timer");
                timer.setConfiguration(Collections.singletonMap("cron", timerCondition.getCron()));

                DeviceMessageSendNode.Config senderConfig = new DeviceMessageSendNode.Config();
                senderConfig.setAsync(true);
                senderConfig.setDeviceId(alarmRule.getDeviceId());
                senderConfig.setProductId(alarmRule.getProductId());
                senderConfig.setMessage(msg.toJson());

                RuleNodeModel messageSender = new RuleNodeModel();
                messageSender.setId("message-sender:" + (++index));
                messageSender.setName("定时发送设备消息");
                messageSender.setExecutor("device-message-sender");
                messageSender.setConfiguration(FastBeanCopier.copy(senderConfig, new HashMap<>()));

                RuleLink link = new RuleLink();
                link.setId(timer.getId().concat(":").concat(messageSender.getId()));
                link.setName("执行动作:" + index);
                link.setSource(timer);
                link.setTarget(messageSender);
                timer.getOutputs().add(link);
                messageSender.getInputs().add(link);
                model.getNodes().add(timer);
                model.getNodes().add(messageSender);
            }
        }

        RuleNodeModel conditionNode = new RuleNodeModel();
        conditionNode.setId("conditions");
        conditionNode.setName("预警条件");
        conditionNode.setExecutor("device_alarm");
        conditionNode.setConfiguration(Collections.singletonMap("rule", rule.getAlarmRule()));
        model.getNodes().add(conditionNode);
        if (CollectionUtils.isNotEmpty(rule.getAlarmRule().getOperations())) {
            int index = 0;
            for (DeviceAlarmRule.Operation operation : rule.getAlarmRule().getOperations()) {
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
