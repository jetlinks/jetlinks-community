/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
