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
package org.jetlinks.community.web;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.jetlinks.community.resource.Resource;
import org.jetlinks.community.resource.ResourceManager;
import org.jetlinks.community.resource.TypeScriptDeclareResourceProvider;
import org.jetlinks.community.resource.ui.UiResourceProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashMap;

@RestController
@RequestMapping("/system/resources")
@Hidden
@AllArgsConstructor
public class SystemResourcesController {

    private final ResourceManager resourceManager;

    @GetMapping("/ui")
    @SneakyThrows
    @Authorize(merge = false)
    public Flux<Object> getUIResources() {
        return resourceManager
            .getResources(UiResourceProvider.TYPE)
            .map(resource->resource.as(HashMap.class));
    }

    @GetMapping("/{type}")
    @SneakyThrows
    public Flux<String> getResources(@PathVariable String type) {
        return Authentication
            .currentReactive()
            .filter(auth -> "admin".equals(auth.getUser().getUsername()))
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(auth -> resourceManager.getResources(type))
            .map(Resource::asString);
    }


    @GetMapping("/{id}.d.ts")
    @SneakyThrows
    @Authorize
    public Mono<String> getTypeScriptResource(@PathVariable String id) {

        return resourceManager
            .getResources(
                TypeScriptDeclareResourceProvider.TYPE,
                Collections.singleton(id))
            .map(Resource::asString)
            .singleOrEmpty();
    }
}
