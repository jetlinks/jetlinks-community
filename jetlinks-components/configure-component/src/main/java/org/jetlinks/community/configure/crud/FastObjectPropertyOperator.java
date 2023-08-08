package org.jetlinks.community.configure.crud;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.SneakyThrows;
import org.hswebframework.ezorm.core.ApacheCommonPropertyOperator;
import org.hswebframework.ezorm.core.GlobalConfig;
import org.hswebframework.ezorm.core.ObjectPropertyOperator;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.bean.SingleValueMap;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static org.hswebframework.web.bean.FastBeanCopier.include;

@Component
public class FastObjectPropertyOperator implements ObjectPropertyOperator {
    static {
        GlobalConfig.setPropertyOperator(new FastObjectPropertyOperator());
    }

    static final FastThreadLocal<SingleValueMap<Object, Object>> cache = new FastThreadLocal<SingleValueMap<Object, Object>>() {
        @Override
        protected SingleValueMap<Object, Object> initialValue() {
            return new SingleValueMap<>();
        }
    };

    private static SingleValueMap<Object, Object> take() {
        SingleValueMap<Object, Object> map = cache.get();
        if (map.isEmpty()) {
            return map;
        }
        return new SingleValueMap<>();
    }

    @Override
    public Optional<Object> getProperty(Object source, String key) {
        if (key.contains(".") || key.contains("[")) {
            return ApacheCommonPropertyOperator.INSTANCE.getProperty(source, key);
        }
        if (source instanceof Map) {
            return Optional.ofNullable(((Map<?, ?>) source).get(key));
        } else {
            SingleValueMap<Object, Object> map = take();
            try {
                FastBeanCopier.copy(source, map, include(key));
                Object value = map.getValue();
                return Optional.ofNullable(value);
            } finally {
                map.clear();
            }
        }
    }

    @Override
    @SuppressWarnings("all")
    public void setProperty(Object object, String name, Object value) {
        if (name.contains(".") || name.contains("[") || value == null) {
            ApacheCommonPropertyOperator.INSTANCE.setProperty(object, name, value);
            return;
        }
        if (object instanceof Map) {
            ((Map<String, Object>) object).put(name, value);
            return;
        }
        SingleValueMap<Object, Object> map = take();
        try {
            map.put(name, value);
            FastBeanCopier.copy(map, object);
        } finally {
            map.clear();
        }

    }

    @Override
    @SneakyThrows
    public Object getPropertyOrNew(Object object, String name) {
        Object value = getProperty(object, name).orElse(null);
        if (null == value) {
            Class<?> clazz = getPropertyType(object, name).orElse(null);
            if (null == clazz) {
                return null;
            }
            value = clazz.getConstructor().newInstance();
            setProperty(object, name, value);
            //设置新的值可能会被copy,所以重新获取值
            value = getProperty(object, name).orElse(null);
        }
        return value;
    }

    @Override
    public Optional<Class<?>> getPropertyType(Object object, String name) {
        return ObjectPropertyOperator.super.getPropertyType(object, name);
    }

}
