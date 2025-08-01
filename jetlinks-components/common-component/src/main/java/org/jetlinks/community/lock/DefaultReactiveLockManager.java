package org.jetlinks.community.lock;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.Map;

class DefaultReactiveLockManager implements ReactiveLockManager {

    private final Map<String, ReactiveLock> cache = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30))
        .<String, ReactiveLock>build()
        .asMap();

    @Override
    public ReactiveLock getLock(String name) {
        return cache.computeIfAbsent(name, DefaultReactiveLock::new);
    }
}
