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
package org.jetlinks.community.relation.impl.property;

import org.jetlinks.core.things.relation.PropertyOperation;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DetectPropertyOperationStrategy implements PropertyOperationStrategy {

    private final Map<String, PropertyOperation> mappers = new ConcurrentHashMap<>();

    public DetectPropertyOperationStrategy addOperation(String key,
                                                        PropertyOperation operation) {
        mappers.put(key, operation);
        return this;
    }

    @Override
    public boolean isSupported(String key) {
        return get(key) != Mono.empty();
    }

    @Override
    public Mono<Object> get(String key) {
        String[] detect = detect(key);
        if (detect == null || detect.length == 0) {
            return Mono.empty();
        }
        PropertyOperation operation = mappers.get(detect[0]);
        if (null == operation) {
            return Mono.empty();
        }
        String relKey;
        if (detect.length == 1) {
            relKey = detect[0];
        } else {
            relKey = detect[1];
        }
        return operation.get(relKey);
    }

    protected String[] detect(String key) {
        if (key.contains(".")) {
            return key.split("[.]",2);
        }
        return new String[]{key};
    }
}
