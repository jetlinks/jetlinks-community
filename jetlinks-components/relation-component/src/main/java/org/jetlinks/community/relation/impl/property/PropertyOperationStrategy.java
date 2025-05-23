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

import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

public interface PropertyOperationStrategy extends PropertyOperation {

    boolean isSupported(String key);

    static <T> PropertyOperationStrategy simple(Mono<T> object,
                                                Consumer<SimplePropertyOperation<T>> operationConsumer) {
        SimplePropertyOperation<T> strategy;
        operationConsumer.accept(strategy = new SimplePropertyOperation<>(object));
        return strategy;
    }

    static PropertyOperationStrategy composite(PropertyOperationStrategy... strategies) {
        return new CompositePropertyOperation(Arrays.asList(strategies));
    }

    static PropertyOperationStrategy detect(Map<String, PropertyOperation> operations) {
        return detect(detect -> operations.forEach(detect::addOperation));
    }

    static PropertyOperationStrategy detect(Consumer<DetectPropertyOperationStrategy> consumer) {
        DetectPropertyOperationStrategy strategy = new DetectPropertyOperationStrategy();
        consumer.accept(strategy);
        return strategy;
    }
}
