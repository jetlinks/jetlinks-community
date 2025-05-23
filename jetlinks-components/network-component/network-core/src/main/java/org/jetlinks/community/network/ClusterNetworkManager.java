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
package org.jetlinks.community.network;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.utils.TransactionUtils;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.community.network.channel.Address;
import org.springframework.boot.CommandLineRunner;
import org.springframework.transaction.reactive.TransactionSynchronization;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.Function3;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

@Slf4j
public class ClusterNetworkManager implements NetworkManager, CommandLineRunner {

    private final NetworkConfigManager configManager;

    private final EventBus eventBus;

    private final Map<String, ReactiveCacheContainer<String, Network>> store = new ConcurrentHashMap<>();

    private final Disposable.Composite disposable = Disposables.composite();

    public ClusterNetworkManager(EventBus eventBus, NetworkConfigManager configManager) {
        this.configManager = configManager;
        this.eventBus = eventBus;
    }

    protected void checkNetwork() {
        Flux.fromIterable(store.values())
            .flatMapIterable(ReactiveCacheContainer::valuesNow)
            .filter(i -> !i.isAlive())
            .flatMap(network -> {
                @SuppressWarnings("all")
                NetworkProvider<Object> provider = (NetworkProvider) NetworkProvider
                    .supports
                    .get(network.getType().getId())
                    .orElse(null);
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
            .subscribe(net -> log.debug("reloaded network :{}", net));
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
            .orElse(null);
        //当前节点不支持此网络组件,则忽略创建.
        //在微服务分布式方式部署时可能会出现.
        if (networkProvider == null) {
            return Mono.empty();
        }

        return configManager
            .getConfig(type, id)
            .filter(NetworkProperties::isEnabled)
            .flatMap(networkProvider::createConfig)
            .flatMap(config -> handler.apply(networkProvider, id, config));
    }

    public void register(NetworkProvider<Object> provider) {
        NetworkProvider.supports.register(provider.getType().getId(), provider);
    }

    @Override
    public List<NetworkProvider<?>> getProviders() {
        return NetworkProvider.supports.getAll();
    }

    @Override
    public Optional<NetworkProvider<?>> getProvider(String type) {
        return NetworkProvider.supports.get(type);
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
        return doReload(type.getId(), id)
            .then(
                TransactionUtils
                    .registerSynchronization(new TransactionSynchronization() {
                        @Override
                        @Nonnull
                        public Mono<Void> afterCommit() {
                            return eventBus
                                .publish("/_sys/network/" + type.getId() + "/reload", id)
                                .then();
                        }
                    }, TransactionSynchronization::afterCommit));

    }

    @Override
    public Mono<Void> shutdown(NetworkType type, String id) {
        return this
            .doShutdown(type.getId(), id)
            .then(eventBus.publish("/_sys/network/" + type.getId() + "/shutdown", id))
            .then();
    }

    @Override
    public Mono<Void> destroy(NetworkType type, String id) {
        return this
            .doDestroy(type.getId(), id)
            .then(eventBus.publish("/_sys/network/" + type.getId() + "/destroy", id))
            .then();
    }

    public Flux<Address> getAddresses(NetworkType type, String id,boolean selfServer) {
        return configManager
            .getConfig(type, id, selfServer)
            .flatMap(prop -> this
                .getProvider(type.getId())
                .map(provider -> provider.createConfig(prop))
                .orElse(Mono.empty()))
            .mapNotNull((conf) -> {
                // todo 健康检查
                if (conf instanceof ClientNetworkConfig) {
                    //客户端则返回远程地址
                    return Address.of(((ClientNetworkConfig) conf).getRemoteAddress(), Address.HEALTH_OK);
                }
                if (conf instanceof ServerNetworkConfig) {
                    //服务端返回公共访问地址
                    return Address.of(((ServerNetworkConfig) conf).getPublicAddress(), Address.HEALTH_OK);
                }
                return null;
            });
    }

    @Override
    public void run(String... args) {
        //定时检查网络组件状态
        disposable.add(
            Flux.interval(Duration.ofSeconds(10))
                .subscribe(t -> this.checkNetwork())
        );

        disposable.add(
            eventBus
                .subscribe(
                    Subscription
                        .builder()
                        .subscriberId("network-config-manager")
                        .topics("/_sys/network/*/*")
                        .justBroker()
                        .build(),
                    payload -> {
                        Map<String, String> vars = payload.getTopicVars("/_sys/network/{type}/{action}");
                        String id = payload.bodyToString(true);
                        log.debug("{} network [{}-{}]", vars.get("action"), vars.get("type"), id);
                        if ("reload".equals(vars.get("action"))) {
                            return this
                                .doReload(vars.get("type"), id)
                                .onErrorResume(err -> {
                                    log.error("reload network error", err);
                                    return Mono.empty();
                                });
                        }
                        if ("shutdown".equals(vars.get("action"))) {
                            return doShutdown(vars.get("type"), id);
                        }
                        if ("destroy".equals(vars.get("action"))) {
                            return doDestroy(vars.get("type"), id);
                        }
                        return Mono.empty();
                    }));
    }

    public void shutdown() {
        log.info("shutdown network manager");
        disposable.dispose();
        for (ReactiveCacheContainer<String, Network> value : store.values()) {
            for (Network network : value.valuesNow()) {
                try {
                    network.shutdown();
                } catch (RejectedExecutionException ignore) {

                } catch (Throwable e) {
                    log.warn("shutdown network error", e);
                }
            }
        }
    }

}
