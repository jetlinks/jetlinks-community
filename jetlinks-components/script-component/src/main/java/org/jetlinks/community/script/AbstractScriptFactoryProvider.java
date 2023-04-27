package org.jetlinks.community.script;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractScriptFactoryProvider implements ScriptFactoryProvider {

    private final Set<String> supports = new HashSet<>();

    public AbstractScriptFactoryProvider(String... supports) {
        this.supports.addAll(Arrays.asList(supports));
    }

    @Override
    public boolean isSupport(String langOrMediaType) {
        return supports.contains(langOrMediaType);
    }

    @Override
    public abstract ScriptFactory factory();
}
