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
