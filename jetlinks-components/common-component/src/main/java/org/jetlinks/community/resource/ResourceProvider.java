package org.jetlinks.community.resource;

import reactor.core.publisher.Flux;

import java.util.Collection;

public interface ResourceProvider {

    String getType();

    Flux<Resource> getResources();

    Flux<Resource> getResources(Collection<String> id);

}
