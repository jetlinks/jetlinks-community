package org.jetlinks.community.script.context;

import lombok.AllArgsConstructor;

import javax.script.Bindings;
import javax.script.ScriptContext;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class CompositeExecutionContext implements ExecutionContext {
    private ExecutionContext[] contexts;

    @Override
    public synchronized ExecutionContext merge(ExecutionContext target) {

        contexts = Arrays.copyOf(contexts, contexts.length + 1);
        contexts[contexts.length - 1] = target;

        return this;
    }

    @Override
    public void setBindings(Bindings bindings, int scope) {

    }

    @Override
    public Bindings getBindings(int scope) {

        return null;
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        contexts[contexts.length - 1].setAttribute(name, value, scope);
    }

    @Override
    public Object getAttribute(String name, int scope) {

        return getAttribute(name);
    }

    @Override
    public Object removeAttribute(String name, int scope) {
        for (ExecutionContext context : contexts) {
            if (context.hasAttribute(name)) {
                return context.removeAttribute(name, scope);
            }
        }
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        for (ExecutionContext context : contexts) {
            if (context.hasAttribute(name)) {
                return context.getAttribute(name);
            }
        }
        return null;
    }

    @Override
    public int getAttributesScope(String name) {
        for (ExecutionContext context : contexts) {
            if (context.hasAttribute(name)) {
                return context.getAttributesScope(name);
            }
        }
        return ENGINE_SCOPE;
    }

    @Override
    public boolean hasAttribute(String key) {
        for (ExecutionContext context : contexts) {
            if (context.hasAttribute(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Writer getWriter() {
        for (ExecutionContext context : contexts) {
            Writer writer = context.getWriter();
            if (writer != null) {
                return writer;
            }
        }
        return null;
    }

    @Override
    public Writer getErrorWriter() {
        for (ExecutionContext context : contexts) {
            Writer writer = context.getErrorWriter();
            if (writer != null) {
                return writer;
            }
        }
        return null;
    }

    @Override
    public void setWriter(Writer writer) {

    }

    @Override
    public void setErrorWriter(Writer writer) {

    }

    @Override
    public Reader getReader() {
        for (ExecutionContext context : contexts) {
            Reader reader = context.getReader();
            if (reader != null) {
                return reader;
            }
        }
        return null;
    }

    @Override
    public void setReader(Reader reader) {

    }

    @Override
    public List<Integer> getScopes() {
        return DefaultExecutionContext.scopes;
    }
}
