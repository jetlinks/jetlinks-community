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
        return cache.computeIfAbsent(name, ignore -> new DefaultReactiveLock());
    }
}
