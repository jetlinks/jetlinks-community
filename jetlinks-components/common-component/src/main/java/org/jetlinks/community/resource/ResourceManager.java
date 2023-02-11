package org.jetlinks.community.resource;

import reactor.core.publisher.Flux;

import java.util.Collection;

public interface ResourceManager {

    Flux<Resource> getResources(String type);

    Flux<Resource> getResources(String type, Collection<String> id);

}
