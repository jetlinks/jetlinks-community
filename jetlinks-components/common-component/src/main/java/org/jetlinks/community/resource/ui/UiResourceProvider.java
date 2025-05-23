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
package org.jetlinks.community.resource.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetlinks.community.resource.Resource;
import org.jetlinks.community.resource.ResourceProvider;
import org.jetlinks.community.resource.SimpleResource;
import org.jetlinks.community.utils.ObjectMappers;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class UiResourceProvider implements ResourceProvider {
    public static final String TYPE = "ui";

    private static final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    private List<Resource> cache;

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Flux<Resource> getResources() {
        return Flux.fromIterable(cache == null ? cache = read() : cache);
    }

    @SneakyThrows
    private List<Resource> read() {
        List<Resource> resources = new ArrayList<>();
        try {
            for (org.springframework.core.io.Resource resource : resolver.getResources("classpath*:/ui/*/package.json")) {
                try (InputStream stream = resource.getInputStream()) {
                    String s = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
                    Module m = ObjectMappers.parseJson(s, Module.class);
                    String path = resource.getURL().getPath();
                    String[] parts = path.split("/");
                    if (parts.length > 2) {
                        m.setPath(parts[parts.length - 3] + "/" + parts[parts.length - 2]);
                        resources.add(m.toResource());
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("load ui resource error", e);
        }
        return resources;
    }


    @Override
    public Flux<Resource> getResources(Collection<String> id) {
        return Flux.empty();
    }


    @Getter
    @Setter
    public static class Module {
        private String id;
        private String name;
        private String description;
        private String path;

        public SimpleResource toResource() {
            id = StringUtils.isBlank(id) ? name : id;
            return SimpleResource.of(id, TYPE, ObjectMappers.toJsonString(this));
        }
    }
}