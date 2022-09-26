package org.jetlinks.community.script.nashorn;

import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import jdk.nashorn.internal.runtime.Undefined;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.script.jsr223.JavaScriptFactory;

import javax.script.ScriptEngine;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class NashornScriptFactory extends JavaScriptFactory implements ClassFilter {

    @Override
    protected ScriptEngine createEngine() {
        return new NashornScriptEngineFactory()
            .getScriptEngine(new String[]{"-doe", "--language=es6", "--global-per-engine"},
                             NashornScriptFactory.class.getClassLoader(),
                             this);
    }

    @Override
    public boolean exposeToScripts(String s) {
        return !isDenied(s);
    }

    @Override
    protected boolean valueIsUndefined(Object value) {
        return value == null || value instanceof Undefined;
    }

    @Override
    public Object convertToJavaType(Object value) {
        return convertToJavaObject(value);
    }

    public static Object convertToJavaObject(Object object) {
        if (object instanceof JSObject) {
            return convertJSObject(((JSObject) object));
        }
        if (object instanceof Undefined) {
            return null;
        }
        return object;
    }

    public static Object convertJSObject(JSObject jsObject) {
        if (jsObject.isArray()) {
            return jsObject
                .values()
                .stream()
                .map(obj -> {
                    if (obj instanceof JSObject) {
                        return convertJSObject(((JSObject) obj));
                    }
                    return obj;
                }).collect(Collectors.toList());
        }
        if (jsObject instanceof Map) {
            Map<Object, Object> newMap = new HashMap<>(((Map<?, ?>) jsObject).size());
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) jsObject).entrySet()) {
                Object val = entry.getValue();
                if (val instanceof JSObject) {
                    val = convertJSObject(((JSObject) val));
                }
                newMap.put(entry.getKey(), val);
            }
            return newMap;
        }
        throw new UnsupportedOperationException("unsupported type:" + jsObject);
    }
}
