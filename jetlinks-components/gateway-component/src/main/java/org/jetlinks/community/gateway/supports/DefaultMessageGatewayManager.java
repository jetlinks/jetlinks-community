package org.jetlinks.community.gateway.supports;

import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.MessageGatewayManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultMessageGatewayManager implements MessageGatewayManager, BeanPostProcessor {

    private final Map<String, MessageGateway> cache = new ConcurrentHashMap<>();

    public DefaultMessageGatewayManager(List<MessageGateway> gateways){
        gateways.forEach(this::register);
    }

    @Override
    public Mono<MessageGateway> getGateway(String id) {
        return Mono.justOrEmpty(cache.get(id));
    }

    @Override
    public Flux<MessageGateway> getAllGateway() {
        return Flux.fromIterable(cache.values());
    }

    public void register(MessageGateway gateway) {
        cache.put(gateway.getId(), gateway);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MessageGateway) {
            register(((MessageGateway) bean));
        }
        return bean;
    }
}
