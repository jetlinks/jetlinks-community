package org.jetlinks.community.dashboard;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DashboardManager {

    Flux<Dashboard> getDashboards();

    Mono<Dashboard> getDashboard(String id);

}
