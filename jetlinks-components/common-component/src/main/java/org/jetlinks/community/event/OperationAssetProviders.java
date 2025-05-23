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
package org.jetlinks.community.event;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.Operation;
import org.jetlinks.community.OperationType;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class OperationAssetProviders {

    private static final Map<String, OperationAssetProvider> providers = new ConcurrentHashMap<>();

    public static void register(OperationAssetProvider provider) {
        for (OperationType supportType : provider.getSupportTypes()) {
            OperationAssetProvider old = providers.put(supportType.getId(), provider);

            if (old != null && old != provider) {
                log.warn("operation asset provider [{}] already exists,will be replaced by [{}]", old, provider);
            }

        }
    }

    public static Optional<OperationAssetProvider> lookup(Operation operation) {
        return lookup(operation.getType().getId());
    }

    public static Optional<OperationAssetProvider> lookup(String operationType) {
        return Optional.ofNullable(providers.get(operationType));
    }

}
