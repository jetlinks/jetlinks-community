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
