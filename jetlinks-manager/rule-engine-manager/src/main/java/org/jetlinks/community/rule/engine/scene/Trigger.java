package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.TimerSpec;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.scene.term.limit.ShakeLimitGrouping;
import org.jetlinks.rule.engine.api.model.RuleModel;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class Trigger implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "触发方式")
    @NotNull(message = "error.scene_rule_trigger_cannot_be_null")
    private TriggerType type;

    @Schema(description = "防抖配置")
    private GroupShakeLimit shakeLimit;

    @Schema(description = "[type]为[device]时不能为空")
    private DeviceTrigger device;

    @Schema(description = "[type]为[timer]时不能为空")
    private TimerSpec timer;

    public void validate() {
        Assert.notNull(type, "error.scene_rule_trigger_cannot_be_null");
        if (type == TriggerType.device) {
            Assert.notNull(device, "error.scene_rule_trigger_device_cannot_be_null");
            device.validate();
        } else if (type == TriggerType.timer) {
            Assert.notNull(timer, "error.scene_rule_trigger_timer_cannot_be_null");
            timer.validate();
        }
    }

    public List<Variable> createDefaultVariable() {
        return type == TriggerType.device && device != null
            ? device.createDefaultVariable()
            : Collections.emptyList();
    }

    public static Trigger device(DeviceTrigger device) {
        Trigger trigger = new Trigger();
        trigger.setType(TriggerType.device);
        trigger.setDevice(device);
        return trigger;
    }

    public static Trigger manual() {
        Trigger trigger = new Trigger();
        trigger.setType(TriggerType.manual);
        return trigger;
    }

    @Getter
    @Setter
    public static class GroupShakeLimit extends ShakeLimit {

        //暂时没有实现其他方式
        @Schema(description = "分组类型:device,product,org...")
        @Hidden
        private String groupType;

        public ShakeLimitGrouping<Map<String, Object>> createGrouping() {
            //todo 其他分组方式实现
            return flux -> flux
                .groupBy(map -> map.getOrDefault("deviceId", "null"), Integer.MAX_VALUE);
        }
    }

    void applyModel(RuleModel model, RuleNodeModel sceneNode) {
        if (type == TriggerType.timer) {
            RuleNodeModel timerNode = new RuleNodeModel();
            timerNode.setId("scene:timer");
            timerNode.setName("定时触发场景");
            timerNode.setExecutor("timer");
            //使用最小负载节点来执行定时
            // timerNode.setSchedulingRule(SchedulerSelectorStrategy.minimumLoad());
            timerNode.setConfiguration(FastBeanCopier.copy(timer, new HashMap<>()));
            model.getNodes().add(timerNode);
            //定时->场景
            model.link(timerNode, sceneNode);
        }
        //设备触发
        if (type == TriggerType.device) {
            device.applyModel(model, sceneNode);
        }
    }

}
