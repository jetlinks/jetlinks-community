package org.jetlinks.community.notify.template;


import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.notify.NotifierProvider;
import org.jetlinks.community.relation.utils.VariableSource;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 通知模版,不同的服务商{@link NotifierProvider},{@link TemplateProvider}需要实现不同的模版
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface Template extends Serializable {

    default Optional<VariableDefinition> getVariable(String key) {
        return Optional.ofNullable(this.getVariables().get(key));
    }

    @Nonnull
    default Map<String, VariableDefinition> getVariables() {
        return Collections.emptyMap();
    }

    default String get(Object value,
                       String key,
                       Map<String, Object> context) {

        if (value instanceof String) {
            String strValue = ((String) value);
            if (StringUtils.isBlank(strValue)) {
                value = VariableSource.resolveValue(key, context);
            }
            //value是模版
            else if (strValue.contains("${")) {
                value = render(strValue, context);
            }
        }

        if (value == null) {
            value = VariableSource.resolveValue(key, context);
        }
        return convert(key, value);
    }

    default String get(String key,
                       Map<String, Object> context,
                       Supplier<?> defaultValueSupplier) {
        Object value = VariableSource.resolveValue(key, context);
        if (value == null) {
            value = defaultValueSupplier.get();
        }
        return convert(key, value);
    }

    default Map<String, Object> renderMap(Map<String, Object> context) {
        return Maps.transformEntries(context, (key, value) ->
            Optional.ofNullable(value)
                    .map(val -> VariableSource.of(val).resolveStatic(context))
                    .map(val -> {
                        Optional<VariableDefinition> variableDefOpt = getVariable(key);
                        if (variableDefOpt.isPresent()) {
                            return variableDefOpt.get().convertValue(val);
                        }
                        return val;
                    })
                    .orElse(null)
        );
    }

    default String convert(String key, Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value)
                .stream()
                .map(val -> convert(key, val))
                .collect(Collectors.joining(","));
        }
        VariableDefinition def = getVariable(key).orElse(null);
        if (null != def) {
            return def.convertValue(value);
        }
        return value == null ? "" : String.valueOf(value);
    }

    default List<String> render(Collection<String> templates,
                                Map<String, Object> context) {
        return templates
            .stream()
            .map(str -> render(str, context))
            .collect(Collectors.toList());
    }

    default String render(String templateText,
                          Map<String, Object> context) {
        return TemplateUtils.simpleRender(templateText,
                                          context,
                                          var -> getVariable(var).orElse(null));
    }
}
