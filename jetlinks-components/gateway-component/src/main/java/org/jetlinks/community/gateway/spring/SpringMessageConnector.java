package org.jetlinks.community.gateway.spring;

import org.jetlinks.community.gateway.MessageConnection;
import org.jetlinks.community.gateway.MessageConnector;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SpringMessageConnector implements MessageConnector, BeanPostProcessor {

    private EmitterProcessor<MessageConnection> processor = EmitterProcessor.create(false);

    FluxSink<MessageConnection> connectionSink = processor.sink();

    @Nonnull
    @Override
    public String getId() {
        return "spring";
    }

    @Nullable
    @Override
    public String getName() {
        return "Spring Message Connector";
    }

    @Nonnull
    @Override
    public Flux<MessageConnection> onConnection() {
        return processor.map(Function.identity());
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        ReflectionUtils.doWithMethods(type, method -> {
            AnnotationAttributes subscribes = AnnotatedElementUtils.getMergedAnnotationAttributes(method, Subscribe.class);
            if (CollectionUtils.isEmpty(subscribes)) {
                return;
            }
            SpringMessageConnection connection = new SpringMessageConnection(
                type.getSimpleName().concat(".").concat(method.getName())
                , Stream.of(subscribes.getStringArray("value")).map(Subscription::new).collect(Collectors.toList())
                , new ProxyMessageListener(bean, method),
                subscribes.getBoolean("shareCluster")
            );
            connectionSink.next(connection);
        });

        return bean;
    }
}
