package org.jetlinks.community.dashboard;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 仪表对象: CPU,内存
 */
public interface DashboardObject  {

    ObjectDefinition getDefinition();

    Flux<Measurement> getMeasurements();

    Mono<Measurement> getMeasurement(String id);

}
