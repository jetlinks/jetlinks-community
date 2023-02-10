package org.jetlinks.community.script;

import org.jetlinks.community.script.nashorn.NashornScriptFactoryProvider;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Scripts {
    private final static List<ScriptFactoryProvider> providers = new CopyOnWriteArrayList<>();

    private final static Map<String, ScriptFactory> globals = new ConcurrentHashMap<>();

    static {
        providers.add(new NashornScriptFactoryProvider());

        try {
            for (ScriptFactoryProvider scriptFactoryProvider : ServiceLoader.load(ScriptFactoryProvider.class)) {
                providers.add(scriptFactoryProvider);
            }
        } catch (Throwable ignore) {
        }
    }

    private static ScriptFactoryProvider lookup(String lang) {
        for (ScriptFactoryProvider provider : providers) {
            if (provider.isSupport(lang)) {
                return provider;
            }
        }
        throw new UnsupportedOperationException("unsupported script lang:" + lang);
    }

    public static ScriptFactory getFactory(String lang) {
        return globals.computeIfAbsent(lang, Scripts::newFactory);
    }

    public static ScriptFactory newFactory(String lang) {
        return lookup(lang).factory();
    }
}
