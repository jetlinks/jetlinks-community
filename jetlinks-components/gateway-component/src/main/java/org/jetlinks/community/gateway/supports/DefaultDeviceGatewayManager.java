package org.jetlinks.community.gateway.supports;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.jetlinks.community.network.channel.ChannelInfo;
import org.jetlinks.community.network.channel.ChannelProvider;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class DefaultDeviceGatewayManager implements DeviceGatewayManager {

    private final DeviceGatewayPropertiesManager propertiesManager;

    private final Map<String, DeviceGatewayProvider> providers = new ConcurrentHashMap<>();

    private final ReactiveCacheContainer<String, DeviceGateway> store = ReactiveCacheContainer.create();

    private final Map<String, ChannelProvider> channels = new ConcurrentHashMap<>();

    public void addChannelProvider(ChannelProvider provider) {
        channels.put(provider.getChannel(), provider);
    }

    public void addGatewayProvider(DeviceGatewayProvider provider) {
        providers.put(provider.getId(), provider);
    }

    public DefaultDeviceGatewayManager(DeviceGatewayPropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
    }

    private Mono<DeviceGateway> doGetGateway(String id) {
        if (null == id) {
            return Mono.empty();
        }
        return store
            .computeIfAbsent(id, this::createGateway);
    }


    protected Mono<DeviceGateway> createGateway(String id) {
        return propertiesManager
            .getProperties(id)
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("网关配置[" + id + "]不存在")))
            .flatMap(properties -> getProviderNow(properties.getProvider()).createDeviceGateway(properties));

    }

    public Mono<Void> doShutdown(String gatewayId) {
        return Mono.justOrEmpty(store.remove(gatewayId))
                   .flatMap(DeviceGateway::shutdown)
                   .doOnSuccess(nil -> log.debug("shutdown device gateway {}", gatewayId))
                   .doOnError(err -> log.error("shutdown device gateway {} error", gatewayId, err));
    }

    @Override
    public Mono<Void> shutdown(String gatewayId) {
        return doShutdown(gatewayId);
    }

    public Mono<Void> doStart(String id) {
        return this
            .getGateway(id)
            .flatMap(DeviceGateway::startup)
            .doOnSuccess(nil -> log.debug("started device gateway {}", id))
            .doOnError(err -> log.error("start device gateway {} error", id, err));
    }

    @Override
    public Mono<Void> start(String gatewayId) {
        return this
            .doStart(gatewayId);
    }

    @Override
    public Mono<DeviceGateway> getGateway(String id) {
        return doGetGateway(id);
    }

    @Override
    public Mono<Void> reload(String gatewayId) {
        return this
            .doReload(gatewayId);
    }

    private Mono<Void> doReload(String gatewayId) {
        return propertiesManager
            .getProperties(gatewayId)
            .flatMap(prop -> {

                DeviceGatewayProvider provider = this.getProviderNow(prop.getProvider());
                return store
                    .compute(gatewayId, (id, gateway) -> {
                        if (gateway != null) {
                            log.debug("reload device gateway {} {}:{}", prop.getName(), prop.getProvider(), prop.getId());
                            return provider
                                .reloadDeviceGateway(gateway, prop)
                                .cast(DeviceGateway.class);
                        }
                        log.debug("create device gateway {} {}:{}", prop.getName(), prop.getProvider(), prop.getId());
                        return provider
                            .createDeviceGateway(prop)
                            .flatMap(newer -> newer.startup().thenReturn(newer));
                    });
            })
            .then();
    }

    @Override
    public List<DeviceGatewayProvider> getProviders() {
        return providers
            .values()
            .stream()
            .sorted(Comparator.comparingInt(DeviceGatewayProvider::getOrder))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<DeviceGatewayProvider> getProvider(String provider) {
        return Optional.ofNullable(providers.get(provider));
    }

    public DeviceGatewayProvider getProviderNow(String provider) {
        return DeviceGatewayProviders.getProviderNow(provider);
    }

    @Override
    public Mono<ChannelInfo> getChannel(String channel, String channelId) {
        if (!StringUtils.hasText(channel) || !StringUtils.hasText(channel)) {
            return Mono.empty();
        }
        return Mono.justOrEmpty(channels.get(channel))
                   .flatMap(provider -> provider.getChannelInfo(channelId));
    }

}
