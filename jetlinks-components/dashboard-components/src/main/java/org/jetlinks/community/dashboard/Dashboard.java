package org.jetlinks.community.dashboard;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface Dashboard {

    DashboardDefinition getDefinition();

    Flux<DashboardObject> getObjects();

    Mono<DashboardObject> getObject(String id);

}
