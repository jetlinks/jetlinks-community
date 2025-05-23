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
package org.jetlinks.community.gateway.spring;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.utils.TemplateParser;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.event.Subscription;
import org.jetlinks.core.event.TopicPayload;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.trace.MonoTracer;
import org.jetlinks.community.gateway.annotation.Subscribe;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpringMessageBroker implements BeanPostProcessor, ApplicationContextAware, SmartInitializingSingleton {

    private ApplicationContext context;

    private final List<Dispatcher> dispatchers = new ArrayList<>();

    public SpringMessageBroker(ApplicationContext context) {
        this.context = context;
    }

    public SpringMessageBroker() {

    }

    @Override
    public Object postProcessAfterInitialization(@Nonnull Object bean, @Nonnull String beanName) throws BeansException {
        Class<?> type = ClassUtils.getUserClass(bean);
        ReflectionUtils.doWithMethods(type, method -> {
            AnnotationAttributes subscribes = AnnotatedElementUtils.getMergedAnnotationAttributes(method, Subscribe.class);
            if (CollectionUtils.isEmpty(subscribes)) {
                return;
            }
            dispatchers.add(new Dispatcher(bean, type, method, context, subscribes));
        });

        return bean;
    }

    private static class Dispatcher implements Function<TopicPayload, Mono<Void>> {
        private final ApplicationContext context;
        private final ProxyMessageListener listener;

        private final Subscription subscription;

        private final MonoTracer<Void> tracer;
        private final String callName;

        public Dispatcher(Object bean,
                          Class<?> type,
                          Method method,
                          ApplicationContext context,
                          AnnotationAttributes subscribes) {
            this.context = context;
            String id = subscribes.getString("id");
            if (!StringUtils.hasText(id)) {
                id = type.getSimpleName().concat(".").concat(method.getName());
            }
            String traceName = "/java/" + type.getSimpleName() + "/" + method.getName();
            callName = type.getSimpleName() + "." + method.getName();
            subscription = Subscription
                .builder()
                .subscriberId("spring:" + id)
                .topics(Arrays.stream(subscribes.getStringArray("value"))
                              .map(this::convertTopic)
                              .collect(Collectors.toList()))
                .priority(subscribes.getNumber("priority"))
                .features((Subscription.Feature[]) subscribes.get("features"))
                .build();
            listener = new ProxyMessageListener(bean, method);
            tracer = MonoTracer.create(SharedPathString.of(traceName));
        }


        protected String convertTopic(String topic) {
            if (!topic.contains("${")) {
                return topic;
            }
            return TemplateParser.parse(topic, template -> {
                String[] arr = template.split(":", 2);
                String property = context.getEnvironment().getProperty(arr[0], arr.length > 1 ? arr[1] : "");
                if (!StringUtils.hasText(property)) {
                    throw new IllegalArgumentException("Parse topic [" + template + "] error, can not get property : " + arr[0]);
                }
                return property;
            });
        }

        private void logError(Throwable err) {
            if(err instanceof BeansException){
                return;
            }
            log.error("handle[{}] event message error : {}", listener, err.getLocalizedMessage(), err);
        }

        void start() {
            context
                .getBean(EventBus.class)
                .subscribe(subscription, this);
        }

        @Override
        public Mono<Void> apply(TopicPayload msg) {
            try {
                return listener
                    .onMessage(msg)
                    .as(tracer)
                    .doOnError(this::logError)
                    .checkpoint(callName);
            } catch (Throwable e) {
                logError(e);
            }
            return Mono.empty();
        }
    }

    @Override
    public void setApplicationContext(@Nonnull ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (Dispatcher dispatcher : dispatchers) {
            dispatcher.start();
        }
    }
}
