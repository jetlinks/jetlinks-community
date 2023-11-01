package org.jetlinks.community.relation.utils;

import com.google.common.collect.Maps;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.relation.RelationManagerHolder;
import org.jetlinks.core.config.ConfigKey;
import org.jetlinks.reactor.ql.supports.DefaultPropertyFeature;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 变量值来源描述
 *
 * @author zhouhao
 * @since 2.0
 */
@Getter
@Setter
public class VariableSource implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "来源")
    private Source source;

    @Schema(description = "固定值,[source]为[fixed]时不能为空")
    private Object value;

    @Schema(description = "上游key,[source]为[upper]时不能为空")
    private String upperKey;

    @Schema(description = "关系,[source]为[relation]时不能为空")
    private VariableObjectSpec relation;

    @Schema(description = "拓展信息")
    private Map<String, Object> options;

    public Map<String, Object> toMap() {
        return FastBeanCopier.copy(this, new HashMap<>());
    }

    public void validate() {
        Assert.notNull(source, "source can not be null");
        switch (source) {
            case fixed:
                Assert.notNull(value, "value can not be null");
                break;
            case upper:
                Assert.hasText(upperKey, "upperKey can not be empty");
                break;
            case relation:
                Assert.notNull(relation, "relation can not be null");
                relation.validate();
                break;
        }
    }

    public static VariableSource fixed(Object value) {
        VariableSource variableSource = new VariableSource();
        variableSource.setSource(Source.fixed);
        variableSource.setValue(value);
        return variableSource;
    }

    public static VariableSource upper(String key) {
        VariableSource variableSource = new VariableSource();
        variableSource.setSource(Source.upper);
        variableSource.setUpperKey(key);
        return variableSource;
    }

    public static VariableSource relation(VariableObjectSpec spec) {
        VariableSource variableSource = new VariableSource();
        variableSource.setSource(Source.relation);
        variableSource.setRelation(spec);
        return variableSource;
    }

    public static VariableSource of(Object value) {
        if (value instanceof VariableSource) {
            return ((VariableSource) value);
        }
        if (value instanceof Map) {
            Map<?, ?> mapVal = ((Map<?, ?>) value);
            if(!mapVal.containsKey("$noVariable")){
                Object sourceName = mapVal.get("source");
                if (sourceName != null && VariableSource.Source.of(String.valueOf(sourceName)).isPresent()) {
                    VariableSource source = FastBeanCopier.copy(mapVal, new VariableSource());
                    if (source.getSource() != null) {
                        return source;
                    }
                }
            }
        }

        return fixed(value);
    }

    public Flux<Object> resolve(Map<String, Object> context) {
        return this.resolve(context, null);
    }

    public Flux<Object> resolve(Map<String, Object> context,
                                ConfigKey<?> propertyPath) {
        validate();
        if (getSource() == VariableSource.Source.fixed) {
            return value == null ? Flux.empty() : CastUtils.flatStream(Flux.just(value));
        }
        if (getSource() == VariableSource.Source.upper) {
            return Mono
                .justOrEmpty(
                    DefaultPropertyFeature.GLOBAL.getProperty(getUpperKey(), context)
                )
                .flux()
                .as(CastUtils::flatStream);
        }
        if (getSource() == VariableSource.Source.relation) {
            VariableObjectSpec objectSpec = getRelation();
            objectSpec.init(context);
            return RelationManagerHolder
                .getObjects(objectSpec)
                .flatMap(obj -> (propertyPath == null)
                    ? Mono.just(obj.getId())
                    : obj.properties().get(propertyPath));
        }
        return Flux.empty();
    }

    public Object resolveStatic(Map<String, Object> context) {
        validate();
        if (getSource() == VariableSource.Source.fixed) {
            return value;
        }
        if (getSource() == VariableSource.Source.upper) {
            return  DefaultPropertyFeature.GLOBAL.getProperty(getUpperKey(), context).orElse(null);
        }
        return value;
    }

    /**
     * 从上下文中解析出值,上下文支持通过动态变量{@link VariableSource}来指定值的来源.
     *
     * <pre>{@code
     *
     * RelationUtils.resolve("phoneNumber",context,RelationConstants.UserProperty.telephone)
     *
     * }</pre>
     *
     * @param key                  key
     * @param context              上下文
     * @param relationPropertyPath 关系对象属性
     * @return 值
     */
    public static Flux<Object> resolveValue(String key,
                                            Map<String, Object> context,
                                            ConfigKey<String> relationPropertyPath) {
        Object value = getNestProperty(key, context);
        if (value == null) {
            return Flux.empty();
        }
        return CastUtils
            .flatStream(Flux.just(value))
            .map(VariableSource::of)
            .flatMap(source -> source.resolve(context, relationPropertyPath));

    }

    public static Object resolveValue(String key,
                                      Map<String, Object> context) {
        Object value = getNestProperty(key, context);
        if (value == null) {
            return null;
        }
        VariableSource source = of(value);
        if (source.getSource() == VariableSource.Source.fixed) {
            value = source.getValue();
        } else if (source.getSource() == VariableSource.Source.upper) {
            value = getNestProperty(source.getUpperKey(), context);
        } else {
            throw new UnsupportedOperationException("unsupported source type : " + source.getSource());
        }
        return value;
    }

    public static Object getNestProperty(String prop, Map<String, Object> ctx) {
        if (null == prop) {
            return null;
        }
        return DefaultPropertyFeature
            .GLOBAL
            .getProperty(prop, ctx)
            .orElse(null);
    }

    public static Map<String, Object> wrap(Map<String, Object> def,Map<String, Object> context) {
        Map<String, Object> vars = Maps.newLinkedHashMapWithExpectedSize(def.size());

        for (Map.Entry<String, Object> entry : def.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Collection) {
                List<VariableSource> sourceList = ((Collection<?>)value)
                    .stream()
                    .map(obj -> doWrap(obj, context))
                    .collect(Collectors.toList());
                vars.put(key, sourceList);
            } else {
                VariableSource source = doWrap(value, context);
                vars.put(key, source);
            }
        }
        return vars;
    }

    private static VariableSource doWrap(Object value, Map<String, Object> context) {
        VariableSource source = VariableSource.of(value);
        if (source.getSource() == Source.upper) {
            //替换上游值,防止key冲突(source的key和上游的key一样)导致无法获取到真实到上游值
            source = VariableSource.fixed(VariableSource.getNestProperty(source.getUpperKey(), context));
        }
        return source;
    }

    public enum Source {
        //固定值
        fixed,
        //来自上游
        upper,
        //通过关系选择
        relation;

        public static Optional<Source> of(String source) {
            for (Source value : values()) {
                if (value.name().equals(source)) {
                    return Optional.of(value);
                }
            }
            return Optional.empty();
        }
    }
}
