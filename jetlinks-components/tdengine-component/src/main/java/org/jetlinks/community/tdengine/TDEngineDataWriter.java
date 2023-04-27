package org.jetlinks.community.tdengine;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TDEngineDataWriter extends Disposable {

    Mono<Void> write(Point point);

    Mono<Void> write(Flux<Point> points);

}
