package org.jetlinks.community.rule.engine.model;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.rule.engine.device.DeviceAlarmRule;
import org.jetlinks.community.rule.engine.entity.DeviceAlarmEntity;
import org.jetlinks.rule.engine.api.cluster.RunMode;
import org.jetlinks.rule.engine.api.model.RuleLink;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.model.RuleModelParserStrategy;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class DeviceAlarmModelParser implements RuleModelParserStrategy {

    public static String format = "device_alarm";
    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public RuleModel parse(String modelDefineString) {
        DeviceAlarmEntity rule = FastBeanCopier.copy(JSON.parseObject(modelDefineString),DeviceAlarmEntity::new);

        RuleModel model = new RuleModel();
        model.setId("device_alarm:".concat(rule.getId()));
        model.setName(rule.getName());
        model.setRunMode(RunMode.CLUSTER);

        RuleNodeModel conditionNode = new RuleNodeModel();
        conditionNode.setId("conditions");
        conditionNode.setName("预警条件");
        conditionNode.setExecutor("device_alarm");
        conditionNode.setConfiguration(Collections.singletonMap("rule",rule.getAlarmRule()));
        model.getNodes().add(conditionNode);
        if (CollectionUtils.isNotEmpty(rule.getAlarmRule().getOperations())) {
            int index = 0;
            for (DeviceAlarmRule.Operation operation : rule.getAlarmRule().getOperations()) {
                RuleNodeModel action = new RuleNodeModel();
                action.setId("device_alarm_action:" + index);
                action.setName("执行动作:" + index);
                action.setExecutor(operation.getExecutor());
                action.setConfiguration(operation.getConfiguration());

                RuleLink link=new RuleLink();
                link.setId(action.getId().concat(":").concat(conditionNode.getId()));
                link.setName("执行动作:"+index);
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
