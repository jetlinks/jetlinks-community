/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
