/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.configure.redis;

import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;

public class PublishOnReactiveRedisTemplate<K,V> extends ReactiveRedisTemplate<K,V> {
    public PublishOnReactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory,
                                          RedisSerializationContext<K,V> serializationContext) {
        super(connectionFactory, serializationContext);
    }

    public PublishOnReactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory,
                                          RedisSerializationContext<K,V> serializationContext,
                                          boolean exposeConnection) {
        super(connectionFactory, serializationContext, exposeConnection);
    }

    @Override
    @Nonnull
    protected Mono<ReactiveRedisConnection> getConnection() {
        if(Schedulers.isInNonBlockingThread()){
            return super.getConnection();
        }
        // 避免在阻塞线程中获取连接导致序列化缓存失效降低性能
        return super
            .getConnection()
            .publishOn(Schedulers.parallel());
    }
}
