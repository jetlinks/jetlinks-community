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

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.rule.engine.commons.ShakeLimit;
import org.jetlinks.community.rule.engine.commons.ShakeLimitProvider;
import org.jetlinks.community.rule.engine.commons.ShakeLimitResult;
import org.jetlinks.community.rule.engine.commons.impl.SimpleShakeLimitProvider;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.jetlinks.community.rule.engine.scene.SceneRule.SOURCE_ID_KEY;

@Setter
@Getter
public abstract class AbstractSceneTriggerProvider<E extends SceneTriggerProvider.TriggerConfig>
    implements SceneTriggerProvider<E> {


    private String shakeLimitProvider = SimpleShakeLimitProvider.PROVIDER;

    protected String getShakeLimitKey(Map<String, Object> data) {
        return String.valueOf(data.get(SOURCE_ID_KEY));
    }

    @Override
    public final Flux<ShakeLimitResult<Map<String, Object>>> shakeLimit(String key,
                                                                        Flux<Map<String, Object>> source,
                                                                        ShakeLimit limit) {
        return ShakeLimitProvider
            .supports
            .get(shakeLimitProvider)
            .orElse(SimpleShakeLimitProvider.GLOBAL)
            .shakeLimit(key,
                        source.groupBy(this::getShakeLimitKey, Integer.MAX_VALUE),
                        limit);
    }
}
