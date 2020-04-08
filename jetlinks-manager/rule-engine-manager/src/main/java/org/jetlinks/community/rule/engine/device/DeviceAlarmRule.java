package org.jetlinks.community.rule.engine.device;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.rule.engine.api.executor.RuleNodeConfiguration;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.executor.ExecutableRuleNodeFactoryStrategy;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 设备预警规则
 *
 * @author zhouhao
 * @since 1.1
 */
@Getter
@Setter
public class DeviceAlarmRule implements Serializable {

    /**
     * 规则ID
     */
    private String id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 产品ID,不能为空
     */
    private String productId;

    /**
     * 产品名称,不能为空
     */
    private String productName;

    /**
     * 设备ID,当对特定对设备设置规则时,不能为空
     */
    private String deviceId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 类型类型,属性或者事件.
     */
    private MessageType type;

    /**
     * 要单独获取哪些字段信息
     */
    private List<Property> properties;

    /**
     * 执行条件
     */
    private List<Condition> conditions;

    /**
     * 警告发生后的操作,指向其他规则节点,如发送消息通知.
     */
    private List<Operation> operations;


    public List<String> getPlainColumns() {
        Stream<String> conditionColumns = conditions
            .stream()
            .map(condition -> condition.getColumn(type));

        if (CollectionUtils.isEmpty(properties)) {
            return conditionColumns.collect(Collectors.toList());
        }
        return Stream.concat(conditionColumns, properties
            .stream()
            .map(property -> type.getPropertyPrefix() + property.toString()))
            .collect(Collectors.toList());
    }

    @Getter
    @Setter
    public static class Operation implements Serializable {

        /**
         * 执行器
         *
         * @see RuleNodeModel#getExecutor()
         * @see ExecutableRuleNodeFactoryStrategy#getSupportType()
         */
        private String executor;

        /**
         * 执行器配置
         *
         * @see RuleNodeModel#getConfiguration()
         * @see RuleNodeConfiguration
         */
        private Map<String, Object> configuration;
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
        },
        //事件
        event("/device/%s/%s/message/event/%s", "this.data.") {
            @Override
            public String getTopic(String productId, String deviceId, String property) {
                return String.format(getTopicTemplate(), productId, StringUtils.isEmpty(deviceId) ? "*" : deviceId, property);
            }
        };

        private String topicTemplate;

        private String propertyPrefix;

        public abstract String getTopic(String productId, String deviceId, String key);
    }

    @Getter
    @Setter
    public static class Condition implements Serializable {

        //物模型属性或者事件的标识 如: fire_alarm
        private String modelId;

        //过滤条件key 如: temperature.value = ?
        private String key;

        //过滤条件值
        private String value;

        //操作符, 等于,大于,小于....
        private Operator operator = Operator.eq;

        public String getColumn(MessageType type) {
            return type.getPropertyPrefix() + (key.trim()) + " " + (key.trim());
        }

        public String createExpression(MessageType type) {
            return type.getPropertyPrefix() + (key.trim()) + " " + operator.symbol + " ? ";
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
        private String symbol;

    }

    @Getter
    @Setter
    public static class Property implements Serializable {
        private String property;

        private String alias;

        @Override
        public String toString() {
            return property.concat(" ").concat(StringUtils.hasText(alias) ? alias : property);
        }
    }
}
