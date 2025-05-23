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
package org.jetlinks.community.rule.engine.scene;

import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.jetlinks.core.utils.Reactors;
import reactor.bool.BooleanUtils;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
class CompositeSceneFilter implements SceneFilter {

    private final List<SceneFilter> filters;

    @Override
    public Mono<Boolean> filter(SceneData data) {

        if (CollectionUtils.isEmpty(filters)) {
            return Reactors.ALWAYS_TRUE;
        }
        Mono<Boolean> handler = null;
        for (SceneFilter filter : filters) {
            if (handler == null) {
                handler = filter.filter(data);
            } else {
                handler = BooleanUtils.and(handler, handler.filter(Boolean::booleanValue));
            }
        }
        return handler;
    }
}
