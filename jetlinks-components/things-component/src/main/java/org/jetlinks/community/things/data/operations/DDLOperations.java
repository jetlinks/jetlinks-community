package org.jetlinks.community.things.data.operations;

import org.jetlinks.core.things.ThingMetadata;
import reactor.core.publisher.Mono;

public interface DDLOperations {

    Mono<Void> registerMetadata(ThingMetadata metadata);

    Mono<Void> reloadMetadata(ThingMetadata metadata);

}
