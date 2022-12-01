package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.alarm.AlarmTaskExecutorProvider;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.IntType;
import org.jetlinks.core.metadata.types.ObjectType;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.community.rule.engine.executor.DelayTaskExecutorProvider;
import org.jetlinks.community.rule.engine.executor.DeviceMessageSendTaskExecutorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.scene.term.TermTypes;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

import static org.hswebframework.web.i18n.LocaleUtils.*;

/**
 * @see org.jetlinks.community.rule.engine.executor.TimerTaskExecutorProvider
 * @see org.jetlinks.community.rule.engine.executor.DelayTaskExecutorProvider
 * @see org.jetlinks.community.rule.engine.executor.DeviceMessageSendTaskExecutorProvider
 */
@Getter
@Setter
public class SceneAction implements Serializable {

    @Schema(description = "执行器类型")
    @NotNull
    private Executor executor;

    @Schema(description = "执行器类型为[notify]时不能为空")
    private Notify notify;

    @Schema(description = "执行器类型为[delay]时不能为空")
    private Delay delay;

    @Schema(description = "执行器类型为[device]时不能为空")
    private Device device;

    @Schema(description = "执行器类型为[alarm]时不能为空")
    private Alarm alarm;

    @Schema(description = "输出过滤条件,串行执行动作时,满足条件才会进入下一个节点")
    private List<Term> terms;

    @Schema(description = "拓展信息")
    private Map<String,Object> options;

    public Flux<Variable> createVariables(DeviceRegistry registry, Integer branchIndex, int index) {
        //设备
        if (executor == Executor.device && device != null) {
            return device
                .getDeviceMetadata(registry, device.productId)
                .map(metadata -> createVariable(branchIndex, index, device.createVariables(metadata)))
                .flux()
                .as(LocaleUtils::transform);
        }
        if (executor == Executor.alarm && alarm != null) {
            return Mono
                .fromSupplier(() -> createVariable(branchIndex, index, alarm.createVariables()))
                .flux()
                .as(LocaleUtils::transform);
        }
        return Flux.empty();
    }

    private Variable createVariable(Integer branchIndex, int actionIndex, List<Variable> children) {
        int humanIndex = actionIndex + 1;

        String varId = "action_" + humanIndex;

        if (branchIndex != null) {
            varId = "branch_" + branchIndex + "_" + varId;
        }

        String message = resolveMessage(
            "message.action_var_index",
            String.format("动作[%s]", humanIndex),
            humanIndex
        );


        Variable variable = Variable.of(varId, message);
        variable.setChildren(children);

        return variable;
    }

    public static SceneAction notify(String notifyType,
                                     String notifierId,
                                     String templateId,
                                     Consumer<Notify> consumer) {
        SceneAction action = new SceneAction();
        action.executor = Executor.notify;
        action.notify = new Notify();
        action.notify.notifierId = notifierId;
        action.notify.notifyType = notifyType;
        action.notify.templateId = templateId;
        consumer.accept(action.notify);
        return action;
    }


    public void applyNode(RuleNodeModel node) {

        switch (executor) {
            //延迟
            case delay: {
                DelayTaskExecutorProvider.DelayTaskExecutorConfig config = new DelayTaskExecutorProvider.DelayTaskExecutorConfig();
                config.setPauseType(DelayTaskExecutorProvider.PauseType.delay);
                config.setTimeout(delay.time);
                config.setTimeoutUnits(delay.unit.chronoUnit);
                node.setExecutor(DelayTaskExecutorProvider.EXECUTOR);
                node.setConfiguration(FastBeanCopier.copy(config, new HashMap<>()));
                return;
            }
            //通知
            case notify: {
                //NotifierTaskExecutorProvider
                node.setExecutor("notifier");
                Map<String, Object> config = new HashMap<>();
                config.put("notifyType", notify.getNotifyType());
                config.put("notifierId", notify.notifierId);
                config.put("templateId", notify.templateId);
                config.put("variables", notify.variables);
                node.setConfiguration(config);
                return;
            }
            case alarm:
                node.setExecutor(AlarmTaskExecutorProvider.executor);
                node.setConfiguration(FastBeanCopier.copy(alarm, new HashMap<>()));
                return;
            //设备指令
            case device: {
                DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig config = new DeviceMessageSendTaskExecutorProvider.DeviceMessageSendConfig();

                config.setMessage(device.message);

                if (DeviceSelectorProviders.isFixed(device)) {
                    config.setSelectorSpec(FastBeanCopier.copy(device, new DeviceSelectorSpec()));
                } else {
                    config.setSelectorSpec(
                        DeviceSelectorProviders.composite(
                            //先选择产品下的设备
                            DeviceSelectorProviders.product(device.productId),
                            FastBeanCopier.copy(device, new DeviceSelectorSpec())
                        ));
                }

                config.setFrom("fixed");
                config.setStateOperator("direct");
                config.setProductId(device.productId);

                node.setExecutor(DeviceMessageSendTaskExecutorProvider.EXECUTOR);
                node.setConfiguration(config.toMap());
                return;
            }
        }

        throw new UnsupportedOperationException("unsupported executor:" + executor);
    }

    @Getter
    @Setter
    public static class Device extends DeviceSelectorSpec {
        @Schema(description = "产品ID")
        private String productId;

        /**
         * @see FunctionInvokeMessage
         * @see ReadPropertyMessage
         * @see WritePropertyMessage
         */
        @Schema(description = "设备指令")
        private Map<String, Object> message;


        public List<Variable> createVariables(DeviceMetadata metadata) {
            DeviceMessage message = MessageType
                .convertMessage(this.message)
                .filter(DeviceMessage.class::isInstance)
                .map(DeviceMessage.class::cast)
                .orElse(null);
            if (message == null) {
                return Collections.emptyList();
            }
            List<Variable> variables = new ArrayList<>();

            //下发指令是否成功
            variables.add(Variable
                              .of("success",
                                  resolveMessage(
                                      "message.action_execute_success",
                                      "执行是否成功"
                                  ))
                              .withType(BooleanType.ID));

            //设备ID
            variables.add(Variable
                              .of("deviceId",
                                  resolveMessage(
                                      "message.device_id",
                                      "设备ID"
                                  ))
                              .withType(BooleanType.ID)
                              //标识变量属于哪个产品
                              .withOption(Variable.OPTION_PRODUCT_ID, productId)
            );

            if (message instanceof ReadPropertyMessage) {
                List<String> properties = ((ReadPropertyMessage) message).getProperties();
                for (String property : properties) {
                    PropertyMetadata metadata_ = metadata.getPropertyOrNull(property);
                    if (null != metadata_) {
                        variables.add(toVariable("properties",
                                                 metadata_,
                                                 "message.action_var_read_property",
                                                 "读取属性[%s]返回值"));
                    }
                }
            } else if (message instanceof WritePropertyMessage) {
                Map<String, Object> properties = ((WritePropertyMessage) message).getProperties();
                for (String property : properties.keySet()) {
                    PropertyMetadata metadata_ = metadata
                        .getPropertyOrNull(property);
                    if (null != metadata_) {
                        variables.add(toVariable("properties",
                                                 metadata_,
                                                 "message.action_var_write_property",
                                                 "设置属性[%s]返回值"));
                    }
                }
            } else if (message instanceof FunctionInvokeMessage) {
                String functionId = ((FunctionInvokeMessage) message).getFunctionId();
                FunctionMetadata metadata_ = metadata
                    .getFunctionOrNull(functionId);
                if (null != metadata_ && metadata_.getOutput() != null) {
                    variables.add(toVariable("output",
                                             metadata_.getName(),
                                             metadata_.getOutput(),
                                             "message.action_var_function",
                                             "功能调用[%s]返回值",
                                             null));

                }
            }
            return variables;

        }
    }

    private static Variable toVariable(String prefix,
                                       PropertyMetadata metadata,
                                       String i18nKey,
                                       String msgPattern) {
        return toVariable(prefix + metadata.getId(),
                          metadata.getName(),
                          metadata.getValueType(),
                          i18nKey,
                          msgPattern,
                          null);
    }

    private static Variable toVariable(String id,
                                       String metadataName,
                                       DataType dataType,
                                       String i18nKey,
                                       String msgPattern,
                                       String parentName) {

        String fullName = parentName == null ? metadataName : parentName + "." + metadataName;
        Variable variable = Variable.of(id, LocaleUtils.resolveMessage(i18nKey,
                                                                       String.format(msgPattern, fullName),
                                                                       fullName));
        variable.setType(dataType.getType());
        variable.setTermTypes(TermTypes.lookup(dataType));
        if (dataType instanceof ObjectType) {
            List<Variable> children = new ArrayList<>();
            for (PropertyMetadata property : ((ObjectType) dataType).getProperties()) {
                children.add(
                    toVariable(id + "." + property.getId(),
                               property.getName(),
                               property.getValueType(),
                               i18nKey,
                               msgPattern,
                               fullName)
                );
            }
            variable.setChildren(children);
        }

        return variable;

    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Delay implements Serializable {
        @Schema(description = "延迟时间")
        private int time;

        @Schema(description = "时间单位")
        private DelayUnit unit;
    }

    @Getter
    @Setter
    public static class Notify implements Serializable {
        @Schema(description = "通知类型")
        @NotBlank(message = "error.scene_rule_actions_notify_type_cannot_be_empty")
        private String notifyType;

        @Schema(description = "通知配置ID")
        @NotBlank(message = "error.scene_rule_actions_notify_id_cannot_be_empty")
        private String notifierId;

        @Schema(description = "通知模版ID")
        @NotBlank(message = "error.scene_rule_actions_notify_template_cannot_be_blank")
        private String templateId;

        /**
         * 变量值的格式可以为{@link  VariableSource}
         */
        @Schema(description = "通知变量")
        @NotBlank(message = "error.scene_rule_actions_notify_variables_cannot_be_blank")
        private Map<String, Object> variables;
    }


    @Getter
    @Setter
    public static class Alarm extends AlarmTaskExecutorProvider.Config {

        /**
         * @see org.jetlinks.community.rule.engine.alarm.AlarmRuleHandler.Result
         */
        public List<Variable> createVariables() {

            List<Variable> variables = new ArrayList<>();

            variables.add(
                Variable.of("alarmName",
                            LocaleUtils.resolveMessage("message.alarm_config_name", "告警配置名称"))
            );

            variables.add(
                Variable.of("level",
                            LocaleUtils.resolveMessage("message.alarm_level", "告警级别"))
                        .withType(IntType.ID)
            );

            variables.add(
                Variable.of("alarming",
                            LocaleUtils.resolveMessage("message.is_alarming", "是否正在告警"))
                        .withType(BooleanType.ID)
            );

            return variables;
        }
    }


    @Getter
    @AllArgsConstructor
    public enum DelayUnit {
        seconds(ChronoUnit.SECONDS),
        minutes(ChronoUnit.MINUTES),
        hours(ChronoUnit.HOURS);
        final ChronoUnit chronoUnit;

    }

    public enum Executor {
        notify,
        delay,
        device,
        alarm
    }

}