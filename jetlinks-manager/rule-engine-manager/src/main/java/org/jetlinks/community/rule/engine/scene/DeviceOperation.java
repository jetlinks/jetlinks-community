/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.rule.engine.scene;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.core.message.property.WritePropertyMessage;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.types.BooleanType;
import org.jetlinks.core.metadata.types.DateTimeType;
import org.jetlinks.core.metadata.types.UnknownType;
import org.jetlinks.core.things.ThingMetadata;
import org.jetlinks.community.TimerSpec;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.utils.TermColumnUtils;
import org.springframework.util.Assert;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetlinks.core.metadata.SimplePropertyMetadata.of;

@Getter
@Setter
public class DeviceOperation {

    //当前值
    public static final String property_value_type_current = "current";

    @NotNull(message = "error.scene_rule_trigger_device_operation_cannot_be_null")
    private Operator operator;

    @Schema(description = "[operator]为[readProperty,writeProperty,invokeFunction]时不能为空")
    private TimerSpec timer;

    @Schema(description = "[operator]为[reportEvent]时不能为空")
    private String eventId;

    @Schema(description = "[operator]为[readProperty]时不能为空")
    private List<String> readProperties;

    @Schema(description = "[operator]为[writeProperty]时不能为空")
    private Map<String, Object> writeProperties;

    @Schema(description = "[operator]为[invokeFunction]时不能为空")
    private String functionId;

    @Schema(description = "[operator]为[invokeFunction]时不能为空")
    private List<FunctionParameter> functionParameters;


    public static DeviceOperation reportProperty() {
        DeviceOperation operation = new DeviceOperation();
        operation.setOperator(Operator.reportProperty);
        return operation;
    }

    public static DeviceOperation invokeFunction(TimerSpec timer, String functionId, List<FunctionParameter> parameters) {
        DeviceOperation operation = new DeviceOperation();
        operation.setOperator(Operator.invokeFunction);
        operation.setFunctionId(functionId);
        operation.setTimer(timer);
        operation.setFunctionParameters(parameters);
        return operation;
    }

    public Map<String, Object> toMessageTemplate() {
        switch (operator) {
            case readProperty:
                return new ReadPropertyMessage()
                    .addProperties(readProperties)
                    .toJson();
            case writeProperty: {
                WritePropertyMessage message = new WritePropertyMessage();
                message.setProperties(writeProperties);
                return message.toJson();
            }
            case invokeFunction:
                FunctionInvokeMessage message = new FunctionInvokeMessage();
                message.functionId(functionId);
                message.setInputs(functionParameters);
                return message.toJson();
        }

        throw new UnsupportedOperationException("unsupported operator : " + operator);
    }

    /**
     * 解析支持的条件判断列
     *
     * @param metadata 物模型
     * @return 条件列
     */
    public List<TermColumn> parseTermColumns(ThingMetadata metadata) {


        List<TermColumn> terms = new ArrayList<>(32);
        //服务器时间 // _now
        terms.add(TermColumn.of("_now",
                                "message.scene_term_column_now",
                                "服务器时间",
                                DateTimeType.GLOBAL,
                                "收到设备数据时,服务器的时间."));
        //数据上报时间 // timestamp
        terms.add(TermColumn.of("timestamp",
                                "message.scene_term_column_timestamp",
                                "数据上报时间",
                                DateTimeType.GLOBAL,
                                "设备上报的数据中指定的时间."));

        //下发指令操作可以判断结果
        if (operator == Operator.readProperty
            || operator == Operator.writeProperty
            || operator == Operator.invokeFunction) {
            terms.add(TermColumn.of("success",
                                    "message.scene_term_column_event_success",
                                    "场景触发是否成功",
                                    BooleanType.GLOBAL));
        }
        //属性相关
        if (operator == Operator.readProperty
            || operator == Operator.reportProperty
            || operator == Operator.writeProperty) {
            terms.addAll(
                TermColumnUtils.createTerm(
                    metadata.getProperties(),
                    (property, column) -> column.setChildren(TermColumnUtils.createTermColumn("properties",
                                                                                              property,
                                                                                              true,
                                                                                              PropertyValueType.values())),
                    LocaleUtils.resolveMessage("message.device_metadata_property", "属性"))
            );
        } else {
            //其他操作只能获取属性的上一次的值
            terms.addAll(
                TermColumnUtils.createTerm(
                    metadata.getProperties(),
                    (property, column) -> column.setChildren(
                        TermColumnUtils.createTermColumn(
                            "properties",
                            property,
                            true,
                            PropertyValueType.last, PropertyValueType.lastTime)),
                    LocaleUtils.resolveMessage("message.device_metadata_property", "属性")));
        }

        //事件上报
        if (operator == Operator.reportEvent) {
            terms.addAll(
                TermColumnUtils.createTerm(
                    metadata.getEvent(eventId)
                            .<List<PropertyMetadata>>map(event -> Collections
                                .singletonList(
                                    of("data",
                                       event.getName(),
                                       event.getType())
                                ))
                            .orElse(Collections.emptyList()),
                    (property, column) -> column.setChildren(TermColumnUtils.createTermColumn("event", property, false)),
                    LocaleUtils.resolveMessage("message.device_metadata_event", "事件")));
        }
        //调用功能
        if (operator == Operator.invokeFunction) {
            terms.addAll(
                TermColumnUtils.createTerm(
                    metadata.getFunction(functionId)
                            //过滤掉异步功能和无返回值功能的参数输出
                            .filter(fun -> !fun.isAsync() && !(fun.getOutput() instanceof UnknownType))
                            .<List<PropertyMetadata>>map(meta -> Collections.singletonList(
                                of("output",
                                   meta.getName(),
                                   meta.getOutput()))
                            )
                            .orElse(Collections.emptyList()),
                    (property, column) -> column.setChildren(TermColumnUtils.createTermColumn("function", property, false)),
                    LocaleUtils.resolveMessage("message.device_metadata_function", "功能调用")));
        }

        return TermColumn.refactorTermsInfo("properties", terms);
    }

    public void validate() {
        Assert.notNull(operator, "error.scene_rule_trigger_device_operation_cannot_be_null");
        switch (operator) {
            case online:
            case offline:
            case reportProperty:
                return;
            case reportEvent:
                Assert.hasText(eventId, "error.scene_rule_trigger_device_operation_event_id_cannot_be_null");
                return;
            case readProperty:
                Assert.notEmpty(readProperties,
                                "error.scene_rule_trigger_device_operation_read_property_cannot_be_empty");
                return;
            case writeProperty:
                Assert.notEmpty(writeProperties,
                                "error.scene_rule_trigger_device_operation_write_property_cannot_be_empty");
                return;
            case invokeFunction:
                Assert.hasText(functionId,
                               "error.scene_rule_trigger_device_operation_function_id_cannot_be_null");
        }
    }

    public enum Operator {
        online,
        offline,
        //事件上报
        reportEvent,
        //属性上报
        reportProperty,
        //读取属性
        readProperty,
        //修改属性
        writeProperty,
        //调用功能
        invokeFunction;

    }

    @AllArgsConstructor
    @Getter
    public enum PropertyValueType {
        current("message.property_value_type_current", null),
        recent("message.property_value_type_recent", null),
        last("message.property_value_type_last", null),
        lastTime("message.property_value_type_last_time", DateTimeType.GLOBAL),
        ;

        private final String key;

        private final DataType dataType;

        public String getName() {
            return LocaleUtils.resolveMessage(key);
        }

        public String getDescription() {
            return LocaleUtils.resolveMessage(key + "_desc");
        }

        public String getNestDescription(String parentName) {
            String key = this.key + "_nest_desc";
            return LocaleUtils.resolveMessage(key, key, parentName);
        }
    }
}
