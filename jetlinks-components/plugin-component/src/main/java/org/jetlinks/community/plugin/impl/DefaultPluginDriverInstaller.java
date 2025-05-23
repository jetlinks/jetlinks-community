package org.jetlinks.community.plugin.impl;

import org.jetlinks.plugin.core.PluginDriver;
import org.jetlinks.community.plugin.PluginDriverConfig;
import org.jetlinks.community.plugin.PluginDriverInstaller;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultPluginDriverInstaller implements PluginDriverInstaller {

    private final Map<String, PluginDriverInstallerProvider> providers = new ConcurrentHashMap<>();

    public void addProvider(PluginDriverInstallerProvider provider) {
        providers.put(provider.provider(), provider);
    }

    @Override
    public Mono<PluginDriver> install(PluginDriverConfig config) {
        PluginDriverInstallerProvider provider = providers.get(config.getProvider());
        if (null == provider) {
            return Mono.error(() -> new UnsupportedOperationException("unsupported plugin provider:" + config.getProvider()));
        }
        return provider.install(config);
    }

    @Override
    public Mono<PluginDriver> reload(PluginDriver driver, PluginDriverConfig config) {
        PluginDriverInstallerProvider provider = providers.get(config.getProvider());
        if (null == provider) {
            return Mono.error(() -> new UnsupportedOperationException("unsupported plugin provider:" + config.getProvider()));
        }
        return provider.reload(driver,config);
    }

    @Override
    public Mono<Void> uninstall(PluginDriverConfig config) {
        PluginDriverInstallerProvider provider = providers.get(config.getProvider());
        if (provider == null) {
            return Mono.empty();
        }
        return provider.uninstall(config);
    }
}
