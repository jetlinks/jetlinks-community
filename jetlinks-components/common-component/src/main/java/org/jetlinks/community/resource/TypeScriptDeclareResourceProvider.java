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
package org.jetlinks.community.resource;

import org.apache.commons.lang.StringUtils;
import org.jetlinks.core.utils.TypeScriptUtils;
import reactor.core.publisher.Flux;

import java.util.Collection;

public class TypeScriptDeclareResourceProvider implements ResourceProvider {

    public static String TYPE = "typescript-declare";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Flux<Resource> getResources() {
        return Flux.empty();
    }

    @Override
    public Flux<Resource> getResources(Collection<String> ids) {

        return Flux
            .fromIterable(ids)
            .mapNotNull(id -> {
                try {
                    String resource = TypeScriptUtils.loadDeclare(id);
                    if (StringUtils.isEmpty(resource)) {
                        return null;
                    }
                    return SimpleResource.of(
                        id,
                        getType(),
                        resource
                    );
                } catch (Throwable err) {
                    return null;
                }
            });
    }
}
