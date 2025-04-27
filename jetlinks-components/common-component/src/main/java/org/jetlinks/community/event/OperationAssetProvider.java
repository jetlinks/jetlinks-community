package org.jetlinks.community.event;

import org.jetlinks.community.Operation;
import org.jetlinks.community.OperationType;
import reactor.core.publisher.Flux;

public interface OperationAssetProvider {

    OperationType[] getSupportTypes();

    Flux<String> createTopics(Operation operation, String original);

    Flux<String> getAssetTypes(String operationType);

}
