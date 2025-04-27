package org.jetlinks.community.timescaledb;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface TimescaleDBDataWriter {

    Mono<Void> save(String metric, Map<String,Object> data);

    Mono<Void> save(String metric, Flux<Map<String,Object>> data);

}
