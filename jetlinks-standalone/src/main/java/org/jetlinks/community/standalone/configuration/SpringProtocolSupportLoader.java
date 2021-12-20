package org.jetlinks.community.standalone.configuration;

import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoader;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class SpringProtocolSupportLoader implements ProtocolSupportLoader,BeanPostProcessor {

    private final Map<String, ProtocolSupportLoaderProvider> providers = new ConcurrentHashMap<>();

    public void register(ProtocolSupportLoaderProvider provider) {
        this.providers.put(provider.getProvider(), provider);
    }
    @Override
    public Mono<? extends ProtocolSupport> load(ProtocolSupportDefinition definition) {
        return Mono.justOrEmpty(this.providers.get(definition.getProvider()))
            .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported provider:" + definition.getProvider())))
            .flatMap((provider) -> provider.load(definition));
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ProtocolSupportLoaderProvider) {
            register(((ProtocolSupportLoaderProvider) bean));
        }
        return bean;
    }
}