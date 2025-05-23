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

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 场景过滤器,实现此接口来对场景数据进行过滤
 *
 * @author zhouhao
 * @since 2.0
 */
public interface SceneFilter {

    /**
     * 执行过滤,如果返回<code>true</code>则表示将执行场景动作.如果返回<code>false</code>则将不执行场景动作
     *
     * @param data 场景数据
     * @return 是否执行场景动作
     */
    Mono<Boolean> filter(SceneData data);


    static SceneFilter composite(Iterable<SceneFilter> filters) {
        return composite(
            StreamSupport.stream(filters.spliterator(), false)
                         .collect(Collectors.toList())
        );
    }

    static SceneFilter composite(List<SceneFilter> filters) {
        return new CompositeSceneFilter(Collections.unmodifiableList(filters));
    }

}
