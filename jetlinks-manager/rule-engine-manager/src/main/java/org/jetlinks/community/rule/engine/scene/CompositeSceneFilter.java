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
