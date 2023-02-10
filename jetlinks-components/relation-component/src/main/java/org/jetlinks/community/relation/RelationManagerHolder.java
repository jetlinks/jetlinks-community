package org.jetlinks.community.relation;

import org.jetlinks.core.things.relation.ObjectSpec;
import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.RelationManager;
import org.jetlinks.core.things.relation.RelationObject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

public class RelationManagerHolder {

    static RelationManager hold;

    public static Mono<RelationObject> getObject(String objectType, String objectId) {
        return hold == null ? Mono.empty() : hold.getObject(objectType, objectId);
    }


    public static Flux<RelationObject> getObjects(String objectType, Collection<String> objectId) {
        return hold == null ? Flux.empty() : hold.getObjects(objectType, objectId);
    }


    public static Flux<RelationObject> getObjects(ObjectSpec spec) {
        return hold == null ? Flux.empty() : hold.getObjects(spec);
    }
}
