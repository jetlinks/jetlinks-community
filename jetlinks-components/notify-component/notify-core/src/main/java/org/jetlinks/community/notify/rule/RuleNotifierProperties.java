package org.jetlinks.community.notify.rule;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
@Slf4j
public class RuleNotifierProperties {

    private DefaultNotifyType notifyType;

    private String notifierId;

    private String templateId;

    private Map<String, Object> variables;

    public void initVariable() {
        if (MapUtils.isNotEmpty(variables)) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Collection) {
                    List<VariableSource> sourceList = ((Collection<?>) value)
                        .stream()
                        .map(obj -> transformToVariables(entry.getKey(), obj))
                        .collect(Collectors.toList());
                    entry.setValue(sourceList);
                } else {
                    VariableSource source = transformToVariables(entry.getKey(), value);
                    entry.setValue(source);
                }
            }
        }
    }

    public Map<String, Object> createVariables(RuleData data) {
        Map<String, Object> vars = RuleDataHelper.toContextMap(data);
        if (MapUtils.isNotEmpty(variables)) {
            vars.putAll(VariableSource.wrap(variables,vars));
        }
        return vars;
    }

    private VariableSource transformToVariables(String key,
                                                Object value) {
        VariableSource source = VariableSource.of(value);
        if (source.getSource() == VariableSource.Source.upper
            && Objects.equals(source.getUpperKey(), key)) {
            //上游的key与参数key相同了,可能导致无法获取到上游变量
            log.warn("The upper key [{}] is the same as the parameter key,", key);
        }
        return source;
    }

    public void validate() {
        Assert.notNull(notifyType, "notifyType can not be null");
        Assert.hasText(notifierId, "notifierId can not be empty");
        Assert.hasText(templateId, "templateId can not be empty");

    }
}
