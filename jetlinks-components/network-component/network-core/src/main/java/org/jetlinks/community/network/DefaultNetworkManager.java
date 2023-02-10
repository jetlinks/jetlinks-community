package org.jetlinks.community.network;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认网络管理器
 *
 * @author zhouhao
 */
@Component
@Slf4j
public class DefaultNetworkManager implements NetworkManager, BeanPostProcessor, CommandLineRunner {

    private final NetworkConfigManager configManager;

    private final Map<String, ReactiveCacheContainer<String, Network>> store = new ConcurrentHashMap<>();

    private final Map<String, NetworkProvider<Object>> providerSupport = new ConcurrentHashMap<>();

    public DefaultNetworkManager(NetworkConfigManager configManager) {
        this.configManager = configManager;
    }

    protected void checkNetwork() {
        Flux.fromIterable(store.values())
            .flatMapIterable(ReactiveCacheContainer::valuesNow)
            .filter(i -> !i.isAlive())
            .flatMap(network -> {
                NetworkProvider<Object> provider = providerSupport.get(network.getType().getId());
                if (provider == null || !network.isAutoReload()) {
                    return Mono.empty();
                }
                return configManager
                    .getConfig(network.getType(), network.getId())
                    .filter(NetworkProperties::isEnabled)
                    .flatMap(provider::createConfig)
                    .flatMap(conf -> this.createOrUpdate(provider, network.getId(), conf))
                    .onErrorResume((err) -> {
                        log.warn("reload network [{}] error", network, err);
                        return Mono.empty();
                    });
            })
            .subscribe(net -> log.info("reloaded network :{}", net));
    }

    private ReactiveCacheContainer<String, Network> getNetworkStore(String type) {
        return store.computeIfAbsent(type, _id -> ReactiveCacheContainer.create());
    }

    private ReactiveCacheContainer<String, Network> getNetworkStore(NetworkType type) {
        return getNetworkStore(type.getId());
    }

    private <T extends Network> Mono<T> getNetwork(String type, String id) {
        ReactiveCacheContainer<String, Network> networkMap = getNetworkStore(type);
        return networkMap
            .computeIfAbsent(id, (key) -> handleConfig(NetworkType.of(type), key, this::doCreate))
            .map(n -> (T) n);
    }

    @Override
    public <T extends Network> Mono<T> getNetwork(NetworkType type, String id) {
        return getNetwork(type.getId(), id);
    }

    @Override
    public Flux<Network> getNetworks() {
        return Flux.fromIterable(store.values())
                   .flatMapIterable(ReactiveCacheContainer::valuesNow);
    }

    public Mono<Network> createOrUpdate(NetworkProvider<Object> provider, String id, Object properties) {
        ReactiveCacheContainer<String, Network> networkStore = getNetworkStore(provider.getType());
        return networkStore.compute(id, (key, network) -> {
            if (network == null) {
                return provider.createNetwork(properties);
            }
            return provider.reload(network, properties);
        });
    }

    public Mono<Network> doCreate(NetworkProvider<Object> provider, String id, Object properties) {
        return provider.createNetwork(properties);
    }

    private Mono<Network> handleConfig(NetworkType type,
                                       String id,
                                       Function3<NetworkProvider<Object>, String, Object, Mono<Network>> handler) {
        @SuppressWarnings("all")
        NetworkProvider<Object> networkProvider = (NetworkProvider) this
            .getProvider(type.getId())
            .orElseThrow(() -> new UnsupportedOperationException("不支持的类型:" + type.getName()));

        return configManager
            .getConfig(type, id)
            .filter(NetworkProperties::isEnabled)
            .flatMap(networkProvider::createConfig)
            .flatMap(config -> handler.apply(networkProvider, id, config));
    }

    public void register(NetworkProvider<Object> provider) {
        this.providerSupport.put(provider.getType().getId(), provider);
    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        if (bean instanceof NetworkProvider) {
            register(((NetworkProvider) bean));
        }
        return bean;
    }

    @Override
    public List<NetworkProvider<?>> getProviders() {
        return new ArrayList<>(providerSupport.values());
    }

    @Override
    public Optional<NetworkProvider<?>> getProvider(String type) {
        return Optional.ofNullable(providerSupport.get(type));
    }

    private Mono<Void> doReload(String type, String id) {
        return handleConfig(NetworkType.of(type), id, this::createOrUpdate)
            .then();

//        Optional.ofNullable(getNetworkStore(type).getNow(id))
//                .ifPresent(Network::shutdown);
//
//        return this
//            .getNetwork(type, id)
//            .then();
    }

    public Mono<Void> doShutdown(String type, String id) {
        return Mono
            .justOrEmpty(getNetworkStore(type).getNow(id))
            .doOnNext(Network::shutdown)
            .then();
    }

    public Mono<Void> doDestroy(String type, String id) {
        return Mono
            .justOrEmpty(getNetworkStore(type).remove(id))
            .doOnNext(Network::shutdown)
            .then();
    }

    @Override
    public Mono<Void> reload(NetworkType type, String id) {
        return doReload(type.getId(), id);

    }

    @Override
    public Mono<Void> shutdown(NetworkType type, String id) {
        return this
            .doShutdown(type.getId(), id)
            .then();
    }

    @Override
    public Mono<Void> destroy(NetworkType type, String id) {
        return this
            .doDestroy(type.getId(), id)
            .then();
    }


    @Override
    public void run(String... args) {
        //定时检查网络组件状态
        Flux.interval(Duration.ofSeconds(10))
            .subscribe(t -> this.checkNetwork());
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Generated
    public static class Synchronization implements Serializable {
        private NetworkType type;

        private String id;
    }
}
