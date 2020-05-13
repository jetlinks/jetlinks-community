package org.jetlinks.community.gateway.external;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DefaultMessagingManager implements MessagingManager, BeanPostProcessor {

    private final Map<String, SubscriptionProvider> subProvider = new ConcurrentHashMap<>();

    private final static PathMatcher matcher = new AntPathMatcher();

    @Override
    public Flux<Message> subscribe(SubscribeRequest request) {

        return Flux.defer(() -> {
            for (Map.Entry<String, SubscriptionProvider> entry : subProvider.entrySet()) {
                if (matcher.match(entry.getKey(), request.getTopic())) {
                    return entry.getValue()
                        .subscribe(request)
                        .map(v -> {
                            if (v instanceof Message) {
                                return ((Message) v);
                            }
                            return Message.success(request.getId(), request.getTopic(), v);
                        });
                }
            }

            return Flux.error(new UnsupportedOperationException("不支持的topic"));
        });
    }

    public void register(SubscriptionProvider provider) {
        for (String pattern : provider.getTopicPattern()) {
            subProvider.put(pattern, provider);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof SubscriptionProvider) {
            register(((SubscriptionProvider) bean));
        }
        return bean;
    }
}
