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
package org.jetlinks.community.resource.initialize;

import com.alibaba.fastjson.JSON;
import org.hswebframework.web.authorization.define.AuthorizeDefinitionInitializedEvent;
import org.hswebframework.web.authorization.define.MergedAuthorizeDefinition;
import org.jetlinks.community.resource.Resource;
import org.jetlinks.community.resource.ResourceProvider;
import org.jetlinks.community.resource.SimpleResource;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collection;

public class PermissionResourceProvider implements ResourceProvider {
    public static final String type = "permission";

    private final MergedAuthorizeDefinition definition = new MergedAuthorizeDefinition();

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Flux<Resource> getResources() {
        return Flux
            .fromIterable(definition.getResources())
            .map(def -> SimpleResource.of(def.getId(), getType(), JSON.toJSONString(def)));
    }

    @Override
    public Flux<Resource> getResources(Collection<String> id) {
        return getResources()
            .filter(resource -> id == null || id.contains(resource.getId()));
    }

    @EventListener
    public void handleInitializedEvent(AuthorizeDefinitionInitializedEvent event) {
        definition.merge(event.getAllDefinition());
    }
}
