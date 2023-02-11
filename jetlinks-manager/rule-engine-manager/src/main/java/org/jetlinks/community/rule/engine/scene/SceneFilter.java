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
