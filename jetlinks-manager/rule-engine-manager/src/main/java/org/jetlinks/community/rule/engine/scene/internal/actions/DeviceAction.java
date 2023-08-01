package org.jetlinks.community.rule.engine.scene.internal.actions;

import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.core.metadata.types.UnknownType;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.community.reactorql.term.TermTypes;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.scene.Variable;

import java.util.*;
import java.util.stream.Collectors;

import static org.hswebframework.web.i18n.LocaleUtils.resolveMessage;
import static org.jetlinks.community.rule.engine.scene.SceneAction.parseColumnFromOptions;
import static org.jetlinks.community.rule.engine.scene.SceneAction.toVariable;

@Getter
@Setter
public class DeviceAction extends DeviceSelectorSpec {
    @Schema(description = "产品ID")
    private String productId;

    /**
     * @see FunctionInvokeMessage
     * @see ReadPropertyMessage
     * @see WritePropertyMessage
     */
    @Schema(description = "设备指令")
    private Map<String, Object> message;


    public List<Variable> createVariables(ThingMetadata metadata) {
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
                          .withType(BooleanType.ID)
                          .withOption("bool", BooleanType.GLOBAL)
                          .withTermType(TermTypes.lookup(BooleanType.GLOBAL))
        );

        //设备ID
        variables.add(Variable
                          .of("deviceId",
                              resolveMessage(
                                  "message.device_id",
                                  "设备ID"
                              ))
                          .withType(StringType.ID)
                          //标识变量属于哪个产品
                          .withOption(Variable.OPTION_PRODUCT_ID, productId)
                          .withTermType(TermTypes.lookup(StringType.GLOBAL))
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
            if (null != metadata_
                && metadata_.getOutput() != null
                && !(metadata_.getOutput() instanceof UnknownType)
                && !metadata_.isAsync()) {
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

    public List<String> parseColumns() {
        if (MapUtils.isEmpty(message)) {
            return Collections.emptyList();
        }
        DeviceMessage msg = (DeviceMessage) MessageType.convertMessage(message).orElse(null);

        Collection<Object> readyToParse;

        if (msg instanceof WritePropertyMessage) {
            readyToParse = ((WritePropertyMessage) msg).getProperties().values();
        } else if (msg instanceof FunctionInvokeMessage) {
            readyToParse = Lists.transform(((FunctionInvokeMessage) msg).getInputs(), FunctionParameter::getValue);
        } else {
            return Collections.emptyList();
        }


        return readyToParse
            .stream()
            .flatMap(val -> parseColumnFromOptions(VariableSource.of(val).getOptions()).stream())
            .collect(Collectors.toList());
    }
}