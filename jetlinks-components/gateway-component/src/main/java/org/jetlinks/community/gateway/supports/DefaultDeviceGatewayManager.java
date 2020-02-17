package org.jetlinks.community.gateway.supports;

import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.DeviceGatewayManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultDeviceGatewayManager implements DeviceGatewayManager, BeanPostProcessor {

    private final DeviceGatewayPropertiesManager propertiesManager;

    private Map<String, DeviceGatewayProvider> providers = new ConcurrentHashMap<>();

    private Map<String, DeviceGateway> store = new ConcurrentHashMap<>();

    public DefaultDeviceGatewayManager(DeviceGatewayPropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
    }

    private Mono<DeviceGateway> doGetGateway(String id) {
        if (store.containsKey(id)) {
            return Mono.just(store.get(id));
        }
        return propertiesManager
            .getProperties(id)
            .switchIfEmpty(Mono.error(()->new UnsupportedOperationException("网关配置[" + id + "]不存在")))
            .flatMap(properties -> Mono
                .justOrEmpty(providers.get(properties.getProvider()))
                .switchIfEmpty(Mono.error(()->new UnsupportedOperationException("不支持的网络服务[" + properties.getProvider() + "]")))
                .flatMap(provider -> provider
                    .createDeviceGateway(properties)
                    .flatMap(gateway -> {
                        if (store.containsKey(id)) {
                            return gateway
                                .shutdown()
                                .thenReturn(store.get(id));
                        }
                        store.put(id, gateway);
                        return Mono.justOrEmpty(gateway);
                    })));
    }

    @Override
    public Mono<Void> shutdown(String gatewayId) {
        return Mono.justOrEmpty(store.remove(gatewayId))
            .flatMap(DeviceGateway::shutdown);
    }

    @Override
    public Mono<DeviceGateway> getGateway(String id) {
        return Mono
            .justOrEmpty(store.get(id))
            .switchIfEmpty(doGetGateway(id));
    }

    @Override
    public List<DeviceGatewayProvider> getProviders() {
        return new ArrayList<>(providers.values());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DeviceGatewayProvider) {
            DeviceGatewayProvider provider = ((DeviceGatewayProvider) bean);
            providers.put(provider.getId(), provider);
        }
        return bean;
    }


}
