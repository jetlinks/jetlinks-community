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
