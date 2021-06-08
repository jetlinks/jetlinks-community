package org.jetlinks.community.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认网络管理器
 *
 * @author zhouhao
 */
@Component
@Slf4j
public class DefaultNetworkManager implements NetworkManager, BeanPostProcessor {

    private final NetworkConfigManager configManager;


    private final Map<String, Map<String, Network>> store = new ConcurrentHashMap<>();

    private final Map<String, NetworkProvider<Object>> providerSupport = new ConcurrentHashMap<>();

    public DefaultNetworkManager(NetworkConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public Mono<Void> reload(NetworkType type, String id) {
        return Mono.justOrEmpty(getNetworkStore(type)
            .get(id))
            .doOnNext(Network::shutdown)
            .then(getNetwork(type, id))
            .then();
    }

    @PostConstruct
    public void start() {
        Flux.interval(Duration.ofSeconds(10))
            .subscribe(t -> this.checkNetwork());
    }

    /**
     * 检查网络 把需要加载的网络组件启动起来
     */
    protected void checkNetwork() {
        // 获取并过滤所有停止的网络组件
        // 重新加载启动状态的网络组件
        Flux.fromIterable(store.values())
            .flatMapIterable(Map::values)
            .filter(i -> !i.isAlive())
            .flatMap(network -> {
                NetworkProvider<Object> provider = providerSupport.get(network.getType().getId());
                if (provider == null || !network.isAutoReload()) {
                    return Mono.empty();
                }
                return configManager.getConfig(network.getType(), network.getId())
                    .filter(NetworkProperties::isEnabled)
                    .flatMap(provider::createConfig)
                    .map(conf -> this.doCreate(provider, network.getId(), conf))
                    .onErrorContinue((err, res) -> log.warn("reload network [{}] error", network, err));
            })
            .subscribe(net -> log.info("reloaded network :{}", net));
    }

    private Map<String, Network> getNetworkStore(String type) {
        return store.computeIfAbsent(type, _id -> new ConcurrentHashMap<>());
    }

    private Map<String, Network> getNetworkStore(NetworkType type) {
        return getNetworkStore(type.getId());
    }

    @Override
    public <T extends Network> Mono<T> getNetwork(NetworkType type, String id) {
        Map<String, Network> networkMap = getNetworkStore(type);
        return Mono.justOrEmpty(networkMap.get(id))
            .filter(Network::isAlive)
            .switchIfEmpty(Mono.defer(() -> createNetwork(type, id)))
            .map(n -> (T) n);
    }

    /**
     * 如果store中不存在网络组件就创建，存在就重新加载
     *
     * @param provider   网络组件支持提供商
     * @param id         网络组件唯一标识
     * @param properties 网络组件配置
     * @return 网络组件
     */
    public Network doCreate(NetworkProvider<Object> provider, String id, Object properties) {
        return getNetworkStore(provider.getType()).compute(id, (s, network) -> {
            if (network == null) {
                network = provider.createNetwork(properties);
            } else {
                //单例，已经存在则重新加载
                provider.reload(network, properties);
            }
            return network;
        });
    }

    public Mono<Network> createNetwork(NetworkType type, String id) {
        return Mono.justOrEmpty(providerSupport.get(type.getId()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的类型:" + type.getName())))
            .flatMap(provider -> configManager
                .getConfig(type, id)
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("网络[" + type.getName() + "]配置[" + id + "]不存在")))
                .filter(NetworkProperties::isEnabled)
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("网络[" + type.getName() + "]配置[" + id + "]已禁用")))
                .flatMap(provider::createConfig)
                .map(config -> doCreate(provider, id, config)));
    }

    public void register(NetworkProvider<Object> provider) {
        this.providerSupport.put(provider.getType().getId(), provider);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
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
    public Mono<Void> shutdown(NetworkType type, String id) {

        return Mono.justOrEmpty(getNetworkStore(type).get(id))
            .doOnNext(Network::shutdown)
            .then();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Synchronization implements Serializable {
        private NetworkType type;

        private String id;
    }
}
