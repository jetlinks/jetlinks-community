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

/**
 * 设备网关管理器
 * <p>
 * TCP   UDP   MQTT  CoAP
 *
 * @author zhouhao
 */
@Component
public class DefaultDeviceGatewayManager implements DeviceGatewayManager, BeanPostProcessor {

    private final DeviceGatewayPropertiesManager propertiesManager;

    /**
     * TCP MQTT的设备网关服务提供者
     */
    private final Map<String, DeviceGatewayProvider> providers = new ConcurrentHashMap<>();

    /**
     * 启动状态的设备网关
     */
    private final Map<String, DeviceGateway> store = new ConcurrentHashMap<>();

    public DefaultDeviceGatewayManager(DeviceGatewayPropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
    }

    /**
     * 获取设备网关，有则返回，没有就创建返回
     *
     * @param id 网关ID
     * @return 设备网关
     */
    private Mono<DeviceGateway> doGetGateway(String id) {
        if (store.containsKey(id)) {
            return Mono.just(store.get(id));
        }

        // 数据库查 DeviceGatewayEntity 转换成 DeviceGatewayProperties
        // BeanMap中找provider 找不到就是不支持
        // 创建设备网关
        // double check 防止重复创建
        return propertiesManager
            .getProperties(id)
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("网关配置[" + id + "]不存在")))
            .flatMap(properties -> Mono
                .justOrEmpty(providers.get(properties.getProvider()))
                .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("不支持的网络服务[" + properties.getProvider() + "]")))
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
