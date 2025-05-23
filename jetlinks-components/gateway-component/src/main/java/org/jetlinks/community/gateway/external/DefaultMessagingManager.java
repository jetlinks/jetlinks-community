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
