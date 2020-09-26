package org.jetlinks.community.rule.engine.device;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.function.FunctionInvokeMessage;
import org.jetlinks.core.message.function.FunctionParameter;
import org.jetlinks.core.message.property.ReadPropertyMessage;
import org.jetlinks.community.rule.engine.model.Action;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 设备告警规则
 *
 * @author zhouhao
 * @since 1.1
 */
@Getter
@Setter
public class DeviceAlarmRule implements Serializable {
    private static final long serialVersionUID = -1L;

    /**
     * 规则ID
     */
    @Hidden
    private String id;

    /**
     * 规则名称
     */
    @Hidden
    private String name;

    /**
     * 产品ID,不能为空
     */
    @NotBlank
    @Schema(description = "产品ID")
    private String productId;

    /**
     * 产品名称,不能为空
     */
    @Schema(description = "产品名称")
    private String productName;

    /**
     * 设备ID,当对特定对设备设置规则时,不能为空
     */
    @Schema(description = "设备ID,为空时则对产品下所有设备生效")
    private String deviceId;

    /**
     * 设备名称
     */
    @Schema(description = "设备名称")
    private String deviceName;

    /**
     * 触发条件,不能为空
     */
    @Schema(description = "触发条件")
    private List<Trigger> triggers;

    /**
     * 要单独获取哪些字段信息
     */
    @Schema(description = "自定义字段映射")
    private List<Property> properties;

    /**
     * 警告发生后的操作,指向其他规则节点,如发送消息通知.
     */
    @Schema(description = "执行动作")
    private List<Action> actions;

    /**
     * 防抖限制
     */
    @Schema(description = "防抖限制")
    private ShakeLimit shakeLimit;

    public void validate() {
        if (org.apache.commons.collections.CollectionUtils.isEmpty(getTriggers())) {
            throw new IllegalArgumentException("触发条件不能为空");
        }
        getTriggers().forEach(Trigger::validate);
    }


    @AllArgsConstructor
    @Getter
    public enum MessageType {
        //上线
        online("/device/%s/%s/online", "this.") {
            @Override
            public String getTopic(String productId, String deviceId, String key) {
                return String.format(getTopicTemplate(), productId, StringUtils.isEmpty(deviceId) ? "*" : deviceId);
            }
        },
        //离线
        offline("/device/%s/%s/offline", "this.") {
            @Override
            public String getTopic(String productId, String deviceId, String key) {
                return String.format(getTopicTemplate(), productId, StringUtils.isEmpty(deviceId) ? "*" : deviceId);
            }
        },
        //属性
        properties("/device/%s/%s/message/property/**", "this.properties.") {
            @Override
            public String getTopic(String productId, String deviceId, String key) {
                return String.format(getTopicTemplate(), productId, StringUtils.isEmpty(deviceId) ? "*" : deviceId);
            }

            @Override
            public Optional<DeviceMessage> createMessage(Trigger trigger) {
                ReadPropertyMessage readPropertyMessage = new ReadPropertyMessage();
                readPropertyMessage.setProperties(new ArrayList<>(
                    StringUtils.hasText(trigger.getModelId())
                        ? Collections.singletonList(trigger.getModelId())
                        : Collections.emptyList()));

                return Optional.of(readPropertyMessage);
            }
        },
        //事件
        event("/device/%s/%s/message/event/%s", "this.data.") {
            @Override
            public String getTopic(String productId, String deviceId, String property) {
                return String.format(getTopicTemplate(), productId, StringUtils.isEmpty(deviceId) ? "*" : deviceId, property);
            }
        },
        //功能调用回复
        function("/device/%s/%s/message/function/reply", "this.output.") {
            @Override
            public String getTopic(String productId, String deviceId, String property) {
                return String.format(getTopicTemplate(), productId, StringUtils.isEmpty(deviceId) ? "*" : deviceId);
            }

            @Override
            public Optional<DeviceMessage> createMessage(Trigger trigger) {
                FunctionInvokeMessage message = new FunctionInvokeMessage();
                message.setFunctionId(trigger.getModelId());
                message.setInputs(trigger.getParameters());
                message.setTimestamp(System.currentTimeMillis());
                return Optional.of(message);
            }
        };

        private final String topicTemplate;

        private final String propertyPrefix;

        public abstract String getTopic(String productId, String deviceId, String key);

        public Optional<DeviceMessage> createMessage(Trigger trigger) {
            return Optional.empty();
        }
    }

    @Getter
    @AllArgsConstructor
    public enum TriggerType implements Serializable {
        //设备消息
        device(Arrays.asList(
            MessageType.values()
        )),
        //定时,定时获取只支持获取设备属性和调用功能.
        timer(Arrays.asList(
            MessageType.properties,
            MessageType.function
        ));

        final List<MessageType> supportMessageTypes;

    }

    @Getter
    @Setter
    public static class Trigger implements Serializable {

        //触发方式,定时,设备
        @Schema(description = "触发方式")
        private TriggerType trigger = TriggerType.device;

        //trigger为定时任务时的cron表达式
        @Schema(description = "定时触发cron表达式")
        private String cron;

        //类型,属性或者事件.
        @Schema(description = "触发消息类型")
        private MessageType type;

        //trigger为定时任务并且消息类型为功能调用时
        @Schema(description = "定时调用下发功能指令时的参数")
        private List<FunctionParameter> parameters;

        //物模型属性或者事件的标识 如: fire_alarm
        @Schema(description = "物模型表示,如:属性ID,事件ID")
        private String modelId;

        //过滤条件
        @Schema(description = "条件")
        private List<ConditionFilter> filters;

        public Set<String> toColumns() {

            return Stream.concat(
                (StringUtils.hasText(modelId)
                    ? Collections.singleton(type.getPropertyPrefix() + "this['" + modelId + "'] \"" + modelId + "\"")
                    : Collections.<String>emptySet()).stream(),
                (CollectionUtils.isEmpty(filters)
                    ? Stream.<ConditionFilter>empty()
                    : filters.stream())
                    .map(filter -> filter.getColumn(type)))
                .collect(Collectors.toSet());
        }

        public List<Object> toFilterBinds() {
            return filters == null ? Collections.emptyList() :
                filters.stream()
                    .map(ConditionFilter::convertValue)
                    .collect(Collectors.toList());
        }

        public Optional<String> createExpression() {
            if (CollectionUtils.isEmpty(filters)) {
                return Optional.empty();
            }
            return Optional.of(
                filters.stream()
                    .map(filter -> filter.createExpression(type))
                    .collect(Collectors.joining(" and "))
            );
        }

        public void validate() {
            if (type == null) {
                throw new IllegalArgumentException("类型不能为空");
            }

            if (type != MessageType.online && type != MessageType.offline && StringUtils.isEmpty(modelId)) {
                throw new IllegalArgumentException("属性/事件/功能ID不能为空");
            }

            if (trigger == TriggerType.timer) {
                if (StringUtils.isEmpty(cron)) {
                    throw new IllegalArgumentException("cron表达式不能为空");
                }
                try {
                    new CronSequenceGenerator(cron);
                } catch (Exception e) {
                    throw new IllegalArgumentException("cron表达式格式错误", e);
                }
            }
            if (!CollectionUtils.isEmpty(filters)) {
                filters.forEach(ConditionFilter::validate);
            }
        }
    }

    /**
     * 抖动限制
     * <a href="https://github.com/jetlinks/jetlinks-community/issues/8">https://github.com/jetlinks/jetlinks-community/issues/8</a>
     *
     * @since 1.3
     */
    @Getter
    @Setter
    public static class ShakeLimit implements Serializable {
        @Schema(description = "是否开启防抖")
        private boolean enabled;

        //时间限制,单位时间内发生多次告警时,只算一次。单位:秒
        @Schema(description = "时间间隔(秒)")
        private int time;

        //触发阈值,单位时间内发生n次告警,只算一次。
        @Schema(description = "触发阈值(次)")
        private int threshold;

        //当发生第一次告警时就触发,为false时表示最后一次才触发(告警有延迟,但是可以统计出次数)
        @Schema(description = "是否第一次满足条件就触发")
        private boolean alarmFirst;

    }

    @Getter
    @Setter
    public static class ConditionFilter implements Serializable {
        //过滤条件key 如: temperature
        @Schema(description = "条件key")
        private String key;

        //过滤条件值
        @Schema(description = "值")
        private String value;

        //操作符, 等于,大于,小于....
        @Schema(description = "比对方式")
        private Operator operator = Operator.eq;

        public String getColumn(MessageType type) {
            return type.getPropertyPrefix() + "this['" + (key.trim()) + "'] \"" + (key.trim()) + "\"";
        }

        public String createExpression(MessageType type) {
            //函数和this忽略前缀
            if (key.contains("(") || key.startsWith("this")) {
                return key;
            }
            return type.getPropertyPrefix() + "this['" + (key.trim()) + "'] " + operator.symbol + " ? ";
        }

        public Object convertValue() {
            return operator.convert(value);
        }

        public void validate() {
            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("条件key不能为空");
            }
            if (StringUtils.isEmpty(value)) {
                throw new IllegalArgumentException("条件值不能为空");
            }
        }
    }


    @AllArgsConstructor
    @Getter
    public enum Operator {
        eq("="),
        not("!="),
        gt(">"),
        lt("<"),
        gte(">="),
        lte("<="),
        like("like");
        private final String symbol;

        public Object convert(String value) {
            return value;
        }
    }

    @Getter
    @Setter
    public static class Property implements Serializable {
        @Schema(description = "属性")
        private String property;

        @Schema(description = "别名")
        private String alias;

        @Override
        public String toString() {
            return property.concat(" \"").concat(StringUtils.hasText(alias) ? alias : property).concat("\"");
        }
    }
}
