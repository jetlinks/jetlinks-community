package org.jetlinks.community.things.data.operations;

import org.jetlinks.core.message.ThingMessage;
import org.jetlinks.core.metadata.Feature;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public interface SaveOperations {

    Mono<Void> save(ThingMessage thingMessage);

    Mono<Void> save(Collection<? extends ThingMessage> thingMessage);

    Mono<Void> save(Publisher<? extends ThingMessage> thingMessage);

   default Flux<Feature> getFeatures(){
       return Flux.empty();
   }
}
