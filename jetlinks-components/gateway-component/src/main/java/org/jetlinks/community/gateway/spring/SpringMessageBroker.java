package org.jetlinks.community.gateway.spring;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.hswebframework.web.utils.TemplateParser;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.core.utils.TopicUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Slf4j
@AllArgsConstructor
public class SpringMessageBroker implements BeanPostProcessor {

    private final EventBus eventBus;

    private final Environment environment;

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
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
            String traceName = "/java/" + type.getSimpleName() + "/" + method.getName();
            String callName = type.getSimpleName() + "." + method.getName();
            Subscription subscription = Subscription
                .builder()
                .subscriberId("spring:" + id)
                .topics(Arrays.stream(subscribes.getStringArray("value"))
                              .map(this::convertTopic)
                              .collect(Collectors.toList()))
                .priority(subscribes.getNumber("priority"))
                .features((Subscription.Feature[]) subscribes.get("features"))
                .build();

            ProxyMessageListener listener = new ProxyMessageListener(bean, method);

            Consumer<Throwable> logError = error -> log.error("handle[{}] event message error : {}", listener, error.getLocalizedMessage(), error);

            MonoTracer<Void> tracer = MonoTracer.create(traceName);

            eventBus
                .subscribe(subscription, msg -> {
                    try {
                        return listener
                            .onMessage(msg)
                            .as(tracer)
                            .doOnError(logError)
                            .checkpoint(callName);
                    } catch (Throwable e) {
                        logError.accept(e);
                    }
                    return Mono.empty();
                });

        });

        return bean;
    }

    protected String convertTopic(String topic) {
        if (!topic.contains("${")) {
            return topic;
        }
        return TemplateParser.parse(topic, template -> {
            String[] arr = template.split(":", 2);
            String property = environment.getProperty(arr[0], arr.length > 1 ? arr[1] : "");
            if (StringUtils.isEmpty(property)) {
                throw new IllegalArgumentException("Parse topic [" + template + "] error, can not get property : " + arr[0]);
            }
            return property;
        });
    }

}
