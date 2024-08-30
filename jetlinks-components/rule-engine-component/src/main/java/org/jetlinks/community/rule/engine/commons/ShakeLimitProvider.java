package org.jetlinks.community.rule.engine.commons;

import org.jetlinks.community.spi.Provider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.GroupedFlux;

/**
 * 防抖提供商
 *
 * @author zhouhao
 * @since 2.2
 */
public interface ShakeLimitProvider {

    Provider<ShakeLimitProvider> supports = Provider.create(ShakeLimitProvider.class);

    /**
     * @return 提供商唯一标识
     */
    String provider();

    /**
     * 对指定分组数据源进行防抖,并输出满足条件的数据.
     *
     * @param sourceKey 数据源唯一标识
     * @param grouped   分组数据源
     * @param limit     防抖条件
     * @param <T>       数据类型
     * @return 防抖结果
     */
    <T> Flux<ShakeLimitResult<T>> shakeLimit(
        String sourceKey,
        Flux<GroupedFlux<String, T>> grouped,
        ShakeLimit limit);


}
