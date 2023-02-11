package org.jetlinks.community.script.context;

import com.google.common.collect.Maps;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

class DefaultExecutionContext implements ExecutionContext {

    static final List<Integer> scopes = Arrays.asList(ENGINE_SCOPE, GLOBAL_SCOPE);

    private final Map<String, Object>[] ctx;

    private final Function<String, Object> fallback;

    public DefaultExecutionContext(Map<String, Object>[] ctx) {
        this(ctx, ignore -> null);
    }

    public DefaultExecutionContext(Map<String, Object>[] ctx,
                                   Function<String, Object> fallback) {
        this.ctx = Arrays.copyOf(ctx, ctx.length + 1);
        this.fallback = fallback;
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {

    }

    @Override
    public Bindings getBindings(int scope) {
        return null;
    }

    private Map<String, Object> self() {
        Map<String, Object> self = ctx[ctx.length - 1];

        return self == null ?
            ctx[ctx.length - 1] = Maps.newHashMapWithExpectedSize(16)
            : self;
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        self().put(name, value);
    }

    @Override
    public Object getAttribute(String name, int scope) {

        return getAttribute(name);
    }

    @Override
    public Object removeAttribute(String name, int scope) {
        return self().remove(name);
    }

    @Override
    public Object getAttribute(String name) {
        for (Map<String, Object> attr : ctx) {
            if (attr != null && attr.containsKey(name)) {
                return attr.get(name);
            }
        }
        return fallback.apply(name);
    }

    @Override
    public boolean hasAttribute(String key) {
        for (Map<String, Object> attr : ctx) {
            if (attr != null && attr.containsKey(key)) {
                return true;
            }
        }
        return fallback.apply(key) != null;
    }

    @Override
    public int getAttributesScope(String name) {
        return ENGINE_SCOPE;
    }

    @Override
    public Writer getWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer getErrorWriter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWriter(Writer writer) {

    }

    @Override
    public void setErrorWriter(Writer writer) {

    }

    @Override
    public Reader getReader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReader(Reader reader) {

    }

    @Override
    public List<Integer> getScopes() {
        return scopes;
    }
}
