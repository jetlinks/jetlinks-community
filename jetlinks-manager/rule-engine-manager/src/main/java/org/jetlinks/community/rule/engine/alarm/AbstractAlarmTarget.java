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
package org.jetlinks.community.rule.engine.alarm;

import org.jetlinks.community.rule.engine.scene.SceneRule;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.Optional;

public abstract class AbstractAlarmTarget implements AlarmTarget {

    @Override
    public final Flux<AlarmTargetInfo> convert(AlarmData data) {

        return this
            .doConvert(data)
            .doOnNext(info -> {
                String sourceType = AbstractAlarmTarget
                    .getFromOutput(SceneRule.SOURCE_TYPE_KEY, data.getOutput())
                    .map(String::valueOf)
                    .orElse(null);

                String sourceId = AbstractAlarmTarget
                    .getFromOutput(SceneRule.SOURCE_ID_KEY, data.getOutput())
                    .map(String::valueOf)
                    .orElse(null);

                String sourceName = AbstractAlarmTarget
                    .getFromOutput(SceneRule.SOURCE_NAME_KEY, data.getOutput())
                    .map(String::valueOf)
                    .orElse(sourceId);
                if (sourceType != null && sourceId != null) {
                    info.withSource(sourceType, sourceId, sourceName);
                }
            });
    }

    protected abstract Flux<AlarmTargetInfo> doConvert(AlarmData data);

    protected static Optional<Object> getFromOutput(String key, Map<String, Object> output) {
        //优先从场景输出中获取
        Object sceneOutput = output.get(SceneRule.CONTEXT_KEY_SCENE_OUTPUT);
        if (sceneOutput instanceof Map) {
            return Optional.ofNullable(((Map<?, ?>) sceneOutput).get(key));
        }
        return Optional.ofNullable(output.get(key));
    }

}
