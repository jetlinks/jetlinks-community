package org.jetlinks.community.script.nashorn;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.script.jsr223.JavaScriptFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class NashornScriptFactory extends JavaScriptFactory {
    static final Class<?> undefinedClass;

    static final Class<?> jsObjectClass;

    static final Class<?> classFilterClass;

    static final Class<?> scriptFactoryClass;

    static final Method jsIsArrayMethod;
    static final Method jsArrayValuesMethod;
    static final Method getScriptEngineMethod;

    static {

        undefinedClass = loadClass(new String[]{
            "org.openjdk.nashorn.internal.runtime.Undefined",
            "jdk.nashorn.internal.runtime.Undefined"
        });

        jsObjectClass = loadClass(new String[]{
            "org.openjdk.nashorn.api.scripting.JSObject",
            "jdk.nashorn.api.scripting.JSObject"
        });

        classFilterClass = loadClass(new String[]{
            "org.openjdk.nashorn.api.scripting.ClassFilter",
            "jdk.nashorn.api.scripting.ClassFilter"
        });


        scriptFactoryClass = loadClass(new String[]{
            "org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory",
            "jdk.nashorn.api.scripting.NashornScriptEngineFactory"
        });

        jsIsArrayMethod = getMethod(jsObjectClass, "isArray");
        jsArrayValuesMethod = getMethod(jsObjectClass, "values");
        getScriptEngineMethod = getMethod(scriptFactoryClass, "getScriptEngine", String[].class, ClassLoader.class, classFilterClass);
    }

    private static Method getMethod(Class<?> clazz, String name, Class<?>... parameters) {
        if (clazz == null) {
            return null;
        }
        try {
            return clazz.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Class<?> loadClass(String[] classes) {
        for (String clazz : classes) {
            try {
                return Class.forName(clazz);
            } catch (ClassNotFoundException ignore) {
            }
        }
        return null;
    }

    @Override
    @SneakyThrows
    protected ScriptEngine createEngine() {
        ScriptEngineFactory engineFactory = (ScriptEngineFactory) scriptFactoryClass
            .getConstructor()
            .newInstance();

        Object filter = Proxy.newProxyInstance(
            this.getClass().getClassLoader(),
            new Class[]{classFilterClass},
            (proxy, method, args) -> {
                if (Objects.equals(method.getName(), "exposeToScripts")) {
                    return !isDenied((String) args[0]);
                }
                return method.invoke(proxy, args);
            });

        return (ScriptEngine) getScriptEngineMethod
            .invoke(engineFactory,
                    new String[]{"-doe", "--language=es6", "--global-per-engine"},
                    NashornScriptFactory.class.getClassLoader(),
                    filter);

    }

    @Override
    protected boolean valueIsUndefined(Object value) {
        return valueIsUndefined0(value);
    }

    @Override
    public Object convertToJavaType(Object value) {
        return convertToJavaObject(value);
    }

    private static boolean valueIsUndefined0(Object value) {
        if (value == null) {
            return true;
        }
        if (undefinedClass != null) {
            return undefinedClass.isInstance(value);
        }
        return false;
    }

    @SuppressWarnings("all")
    public static Object convertToJavaObject(Object object) {
        if (valueIsUndefined0(object)) {
            return null;
        }
        if (jsIsObject(object)) {
            return convertJSObject(object);
        }
        if (object instanceof Flux) {
            return ((Flux<?>) object)
                .mapNotNull(NashornScriptFactory::convertToJavaObject);
        }
        if (object instanceof Mono) {
            return ((Mono<?>) object)
                .mapNotNull(NashornScriptFactory::convertToJavaObject);
        }

        return object;
    }

    private static boolean jsIsObject(Object obj) {
        return jsObjectClass != null && jsObjectClass.isInstance(obj);
    }

    private static boolean jsIsArray(Object obj) {
        if (jsIsArrayMethod == null) {
            return false;
        }
        try {
            return (boolean) jsIsArrayMethod.invoke(obj);
        } catch (IllegalAccessException | InvocationTargetException ignore) {
        }
        return false;
    }

    @SneakyThrows
    private static Object convertJSObject(Object jsObject) {
        if (jsIsArray(jsObject)) {
            @SuppressWarnings("all")
            Collection<Object> values = (Collection) jsArrayValuesMethod.invoke(jsObject);
            return values
                .stream()
                .map(obj -> {
                    if (jsIsObject(obj)) {
                        return convertJSObject(obj);
                    }
                    return obj;
                })
                .collect(Collectors.toList());
        }
        if (jsObject instanceof Map) {
            Map<Object, Object> newMap = new HashMap<>(((Map<?, ?>) jsObject).size());
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) jsObject).entrySet()) {
                Object val = entry.getValue();
                if (jsIsObject(val)) {
                    val = convertJSObject(val);
                }
                newMap.put(entry.getKey(), val);
            }
            return newMap;
        }
        throw new UnsupportedOperationException("unsupported type:" + jsObject);
    }
}
