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
package org.jetlinks.community.things.data;

import org.jetlinks.community.things.data.operations.SaveOperations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class CacheSaveOperationsStrategy implements ThingsDataRepositoryStrategy {


    private final Map<OperationsContext, SaveOperations> caches = new ConcurrentHashMap<>();

    @Override
    public final SaveOperations opsForSave(OperationsContext context) {
        //save可能是频繁操作,使用cache减少对象创建
        return caches.computeIfAbsent(context, this::createOpsForSave);
    }


    protected abstract SaveOperations createOpsForSave(OperationsContext context);
}
