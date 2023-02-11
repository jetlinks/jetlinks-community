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

    static Optional<Object> getFromOutput(String key, Map<String, Object> output) {
        //优先从场景输出中获取
        Object sceneOutput = output.get(SceneRule.CONTEXT_KEY_SCENE_OUTPUT);
        if (sceneOutput instanceof Map) {
            return Optional.ofNullable(((Map<?, ?>) sceneOutput).get(key));
        }
        return Optional.ofNullable(output.get(key));
    }

}
