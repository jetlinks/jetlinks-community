package org.jetlinks.community.script.context;

import javax.script.ScriptContext;
import java.util.Map;
import java.util.function.Function;

public interface ExecutionContext extends ScriptContext {

    boolean hasAttribute(String key);

    @SafeVarargs
    static ExecutionContext create(Map<String, Object>... context) {
        return new DefaultExecutionContext(context);
    }

    @SafeVarargs
    static ExecutionContext create(Function<String, Object> fallback, Map<String, Object>... context) {
        return new DefaultExecutionContext(context, fallback);
    }

    static ExecutionContext compose(ExecutionContext... contexts) {
        return new CompositeExecutionContext(contexts);
    }

    default ExecutionContext merge(ExecutionContext target) {
        return compose(this, target);
    }


}
