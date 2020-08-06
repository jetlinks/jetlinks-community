package org.jetlinks.community.gateway.spring;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

@Component
@Slf4j
@AllArgsConstructor
public class SpringMessageBroker implements BeanPostProcessor {

    private final EventBus eventBus;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        ReflectionUtils.doWithMethods(type, method -> {
            AnnotationAttributes subscribes = AnnotatedElementUtils.getMergedAnnotationAttributes(method, Subscribe.class);
            if (CollectionUtils.isEmpty(subscribes)) {
                return;
            }
            String id = subscribes.getString("id");
            if (!StringUtils.hasText(id)) {
                id = type.getSimpleName().concat(".").concat(method.getName());
            }
            Subscription subscription = Subscription.of(
                "spring:" + id,
                subscribes.getStringArray("value"),
                (Subscription.Feature[]) subscribes.get("features"));

            ProxyMessageListener listener = new ProxyMessageListener(bean, method);

            eventBus
                .subscribe(subscription)
                .doOnNext(msg ->
                    listener
                        .onMessage(msg)
                        .doOnEach(ReactiveLogger.onError(error -> {
                            log.error(error.getMessage(), error);
                        }))
                        .subscribe()
                )
                .onErrorContinue((err, v) -> log.error(err.getMessage(), err))
                .subscribe();

        });

        return bean;
    }

}
