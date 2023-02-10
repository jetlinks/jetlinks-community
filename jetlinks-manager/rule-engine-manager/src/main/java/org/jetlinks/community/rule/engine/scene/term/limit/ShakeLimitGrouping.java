package org.jetlinks.community.rule.engine.scene.term.limit;

import reactor.core.publisher.Flux;

public interface ShakeLimitGrouping<T> {

    static <T> ShakeLimitGrouping<T> noGroup(){
        return Flux::just;
    }

    Flux<? extends Flux<T>> group(Flux<T> source);

}
