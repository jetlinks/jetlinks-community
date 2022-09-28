package org.jetlinks.community.reference;

import org.jetlinks.community.strategy.StaticStrategyManager;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DefaultDataReferenceManager extends StaticStrategyManager<DataReferenceProvider> implements DataReferenceManager {

    @Override
    public Mono<Boolean> isReferenced(String dataType, String dataId) {
        return this
            .getReferences(dataType, dataId)
            .hasElements();
    }

    @Override
    public Flux<DataReferenceInfo> getReferences(String dataType, String dataId) {
        return doWithFlux(dataType, provider -> provider.getReference(dataId));
    }

    @Override
    public Flux<DataReferenceInfo> getReferences(String dataType) {
        return doWithFlux(dataType, DataReferenceProvider::getReferences);
    }
}
