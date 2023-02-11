package org.jetlinks.community.relation.impl.property;

import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
class CompositePropertyOperation implements PropertyOperationStrategy {

    private final List<PropertyOperationStrategy> strategies;

    @Override
    public boolean isSupported(String key) {
        return getStrategy(key) != null;
    }

    private PropertyOperationStrategy getStrategy(String key) {
        for (PropertyOperationStrategy strategy : strategies) {
            if (strategy.isSupported(key)) {
                return strategy;
            }
        }
        return null;
    }

    @Override
    public Mono<Object> get(String key) {
        PropertyOperationStrategy strategy = getStrategy(key);
        if (strategy == null) {
            return Mono.empty();
        }
        return strategy.get(key);
    }


}
