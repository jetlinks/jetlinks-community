package org.jetlinks.community.rule.engine.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SceneProviders {

    private static final Map<String, SceneActionProvider<?>> actionProviders = new ConcurrentHashMap<>();
    private static final Map<String, SceneTriggerProvider<?>> triggerProviders = new ConcurrentHashMap<>();

    public static void register(SceneActionProvider<?> provider) {
        actionProviders.put(provider.getProvider(), provider);
    }

    public static void register(SceneTriggerProvider<?> provider) {
        triggerProviders.put(provider.getProvider(), provider);
    }

    @SuppressWarnings("all")
    public static <C> Optional<SceneActionProvider<C>> getActionProvider(String provider) {
        return Optional.ofNullable((SceneActionProvider<C>) actionProviders.get(provider));
    }

    public static <C> SceneActionProvider<C> getActionProviderNow(String provider) {
        return SceneProviders
            .<C>getActionProvider(provider)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported SceneActionProvider:" + provider));
    }

    @SuppressWarnings("all")
    public static <C extends SceneTriggerProvider.TriggerConfig> Optional<SceneTriggerProvider<C>> getTriggerProvider(String provider) {
        return Optional.ofNullable((SceneTriggerProvider<C>) triggerProviders.get(provider));
    }

    public static <C extends SceneTriggerProvider.TriggerConfig> SceneTriggerProvider<C> getTriggerProviderNow(String provider) {
        return SceneProviders
            .<C>getTriggerProvider(provider)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported SceneTriggerProvider:" + provider));
    }

    @SuppressWarnings("all")
    public static List<SceneTriggerProvider<SceneTriggerProvider.TriggerConfig>> triggerProviders() {
        return (List) new ArrayList<>(triggerProviders.values());
    }

    public static List<SceneActionProvider<?>> actionProviders() {
        return new ArrayList<>(actionProviders.values());
    }
}
