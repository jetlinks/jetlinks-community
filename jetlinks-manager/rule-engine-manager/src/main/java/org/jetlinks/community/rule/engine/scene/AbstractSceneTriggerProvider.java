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
