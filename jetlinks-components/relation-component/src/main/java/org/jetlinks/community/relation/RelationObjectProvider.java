package org.jetlinks.community.relation;

import org.jetlinks.core.things.relation.ObjectType;
import org.jetlinks.core.things.relation.PropertyOperation;
import reactor.core.publisher.Mono;

public interface RelationObjectProvider {

    String TYPE_USER = "user";

    String TYPE_DEVICE = "device";

    String getTypeId();

    Mono<ObjectType> getType();

    PropertyOperation properties(String id);
}
