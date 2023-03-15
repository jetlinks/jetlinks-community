package org.jetlinks.community.script.jsr223;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.script.AbstractScriptFactory;
import org.jetlinks.community.script.CompiledScript;
import org.jetlinks.community.script.Script;
import org.jetlinks.community.script.context.ExecutionContext;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Compilable;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public abstract class Jsr223ScriptFactory extends AbstractScriptFactory {

    private final ScriptEngine engine;

    public Jsr223ScriptFactory() {
        this.engine = createEngine();
    }


    protected abstract ScriptEngine createEngine();

    @Override
    public final CompiledScript compile(Script script) {
        return compile(script, true);
    }

    private CompiledScript compile(Script script, boolean convert) {

        ExecutionContext ctx = ExecutionContext.create();

        ctx.setAttribute("_$console", new Jsr223ScriptFactory.Console(
                             LoggerFactory.getLogger("org.jetlinks.community.script." + script.getName())),
                         ScriptContext.ENGINE_SCOPE);
        ctx.setAttribute("_$utils", getUtils(), ScriptContext.ENGINE_SCOPE);

        ctx.setAttribute("engine", null, ScriptContext.ENGINE_SCOPE);

        javax.script.CompiledScript compiledScript = compile0(script);

        return (context) -> Jsr223ScriptFactory.this
            .eval(compiledScript,
                  script,
                  ExecutionContext.compose(ctx, context),
                  convert);
    }

    @SneakyThrows
    private Object eval(javax.script.CompiledScript script,
                        Script source,
                        ExecutionContext context,
                        boolean convert) {
        Object res = script.eval(acceptScriptContext(source, context));

        return convert ? convertToJavaType(res) : res;
    }


    protected ExecutionContext acceptScriptContext(Script script, ExecutionContext context) {
        return context;
    }

    @AllArgsConstructor
    public static class Console {
        private final Logger logger;

        public void trace(String text, Object... args) {
            logger.trace(text, args);
        }

        public void warn(String text, Object... args) {
            logger.warn(text, args);
        }

        public void log(String text, Object... args) {
            logger.debug(text, args);
        }

        public void error(String text, Object... args) {
            logger.error(text, args);
        }
    }

    @Override
    @SuppressWarnings("all")
    public final <T> T bind(Script script, Class<T> interfaceType) {
        String returns = createFunctionMapping(interfaceType.getDeclaredMethods());
        String content = script.getContent() + "\n return " + returns + ";";

        CompiledScript compiledScript = compile(script.content(content), false);
        Object source = compiledScript.call(Collections.emptyMap());
        Set<Method> ignoreMethods = new HashSet<>();

        return (T) Proxy.newProxyInstance(
            interfaceType.getClassLoader(),
            new Class[]{interfaceType},
            (proxy, method, args) -> {
                //方法已经被忽略执行
                if (ignoreMethods.contains(method)) {
                    return convertValue(method, null);
                }
                try {
                    return this.convertValue(method,
                                             ((Invocable) engine).invokeMethod(source, method.getName(), args));
                } catch (Throwable e) {
                    if (e instanceof NoSuchMethodException) {
                        log.info("method [{}] undefined in script", method, e);
                        //脚本未定义方法
                        ignoreMethods.add(method);
                    }
                }
                return convertValue(method, null);
            });
    }

    protected boolean valueIsUndefined(Object value) {
        return value == null;
    }

    public Object convertToJavaType(Object value) {
        return value;
    }

    private Object convertValue(Method method, Object value) {
        if (valueIsUndefined(value)) {
            return null;
        }
        value = convertToJavaType(value);

        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            return null;
        }
        if (returnType == int.class) {
            return CastUtils.castNumber(value).intValue();
        }
        if (returnType == float.class) {
            return CastUtils.castNumber(value).floatValue();
        }
        if (returnType == double.class) {
            return CastUtils.castNumber(value).doubleValue();
        }
        if (returnType == long.class) {
            return CastUtils.castNumber(value).longValue();
        }
        if (returnType == byte.class) {
            return CastUtils.castNumber(value).byteValue();
        }
        if (returnType == short.class) {
            return CastUtils.castNumber(value).shortValue();
        }

        return value;
    }

    protected abstract String createFunctionMapping(Method[] methods);

    @SneakyThrows
    private javax.script.CompiledScript compile0(Script script) {
        String rewriteScript = prepare(script);
        log.debug("compile script :\n{}", rewriteScript);
        return ((Compilable) engine).compile(rewriteScript);
    }

    protected String prepare(Script script) {
        return script.getContent();
    }


}
