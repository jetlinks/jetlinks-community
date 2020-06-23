package org.jetlinks.community.rule.engine.executor;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.model.RuleNodeModel;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.LambdaTaskExecutor;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@Slf4j
public class ScriptTaskExecutorProvider implements TaskExecutorProvider {

    @Override
    public String getExecutor() {
        return "script";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new LambdaTaskExecutor("script", context, () -> {

            return createExecutor(context, FastBeanCopier.copy(context.getJob().getConfiguration(), new Config()));

        }));
    }

    @SneakyThrows
    public Function<RuleData, Publisher<?>> createExecutor(ExecutionContext context, Config config) {

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(config.getLang());
        if (engine == null) {
            throw new UnsupportedOperationException("不支持的脚本语言:" + config.getLang());
        }
        if (StringUtils.isEmpty(config.getScript())) {
            log.warn("script is empty");
            return Mono::just;
        }
        String id = DigestUtils.md5Hex(config.getScript());
        if (!engine.compiled(id)) {
            engine.compile(id, config.getScript());
        }

        Handler handler = new Handler();
        Map<String, Object> scriptContext = new HashMap<>();
        scriptContext.put("context", context);
        scriptContext.put("handler", handler);
        engine.execute(id, scriptContext).getIfSuccess();

        return ruleData -> Flux.defer(() -> {
            if (handler.onMessage != null) {
                Object result = handler.onMessage.apply(ruleData);
                if (result == null || result.getClass().getName().equals("jdk.nashorn.internal.runtime.Undefined")) {
                    return Flux.empty();
                }
                if (result instanceof Publisher) {
                    return Flux.from(((Publisher<?>) result));
                }
                if (result instanceof Map) {
                    result = new HashMap<>((Map<?, ?>) result);
                }
                return Flux.just(result);
            }
            return Flux.empty();
        });
    }

    public static class Handler {
        private Function<RuleData, Object> onMessage;

        public void onMessage(Function<RuleData, Object> onMessage) {
            this.onMessage = onMessage;
        }
    }

    @Getter
    @Setter
    public static class Config {

        private String lang = "js";

        private String script;

    }
}
