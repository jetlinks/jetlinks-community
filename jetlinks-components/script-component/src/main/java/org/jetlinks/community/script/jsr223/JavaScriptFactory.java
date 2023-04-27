package org.jetlinks.community.script.jsr223;

import org.jetlinks.community.script.CompiledScript;
import org.jetlinks.community.script.ExposedScript;
import org.jetlinks.community.script.Script;

import javax.script.ScriptContext;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class JavaScriptFactory extends Jsr223ScriptFactory {

    public JavaScriptFactory() {
        super();
    }

    protected final String prepare(Script script) {
        StringJoiner wrap = new StringJoiner("\n");
        //使用匿名函数包装,防止变量逃逸
        wrap.add("(function(){");
        //注入安全性控制代码
        //✨企业版还支持资源限制(防止死循环等操作)
        wrap.add("function exit(){};" +
                     "function Function(e){return function(){}};" +
                     "function quit(){};" +
                     "function eval(s){};" +
                     "this.eval = function(e){};" +
                     "function readFully(){};" +
                     "function readLine(){};" +
                     "const console=_$console;" +
                     "const utils=_$utils;" +
                     "const print = function(e){console.log(e)};" +
                     "const echo = print;");

        wrap.add("/*  script start */");

        wrap.add(script.getContent());

        wrap.add("/*  script end */");
        wrap.add("})()");

        return wrap.toString();
    }

    private final Set<Method> ignoreMethod = new HashSet<>(
        Stream
            .concat(
                Arrays.stream(Object.class.getMethods()),
                Arrays.stream(Callable.class.getMethods())
            )
            .collect(Collectors.toList())
    );

    @Override
    public <T> ExposedScript<T> compileExpose(Script script,
                                              Class<? super T> expose) {
        StringJoiner joiner = new StringJoiner("\n");
        Set<String> distinct = new HashSet<>();
        joiner.add("var _$this = $this;");
        joiner.add(
            Arrays.stream(expose.getMethods())
                  .filter(method -> !ignoreMethod.contains(method))
                  .sorted(Comparator.comparingInt(Method::getParameterCount).reversed())
                  .map(method -> {
                      if (!distinct.add(method.getName())) {
                          return null;
                      }

                      StringBuilder call = new StringBuilder("function ")
                          .append(method.getName())
                          .append("(){");
                      if (method.getParameterCount() == 0) {
                          call.append("return _$this.")
                              .append(method.getName())
                              .append("();");
                      } else if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isArray()) {
                          call.append("return _$this.")
                              .append(method.getName())
                              .append("(utils.toJavaType(arguments));");
                      }else {

                          for (int i = 0; i <= method.getParameterCount(); i++) {
                              String[] args = new String[i];
                              for (int j = 0; j < i; j++) {
                                  args[j] = "arguments[" + j + "]";
                              }
                              String arg = String.join(",", args);
                              call.append("if(arguments.length==").append(i).append("){")
                                  .append("return _$this.")
                                  .append(method.getName())
                                  .append("(").append(arg).append(");")
                                  .append("}");
                          }
                      }

                      call.append("}");

                      return call.toString();
                  })
                  .filter(Objects::nonNull)
                  .collect(Collectors.joining())
        );

        joiner.add(script.getContent());
        CompiledScript compiledScript = compile(script.content(joiner.toString()));

        return (instance, ctx) -> {
            ctx.setAttribute("$this", instance, ScriptContext.ENGINE_SCOPE);
            return compiledScript.call(ctx);
        };
    }

    @Override
    protected String createFunctionMapping(Method[] methods) {
        return Arrays
            .stream(methods)
            .map(Method::getName)
            .map(m -> m + ":typeof(" + m + ")==='undefined'?null:" + m)
            .collect(Collectors.joining(",",
                                        "{", "}"));
    }

}
