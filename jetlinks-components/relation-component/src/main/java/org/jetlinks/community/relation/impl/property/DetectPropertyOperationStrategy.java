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
