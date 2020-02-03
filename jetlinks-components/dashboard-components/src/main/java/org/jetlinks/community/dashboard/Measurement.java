package org.jetlinks.community.dashboard;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 度量,指标. 如: 使用率
 *
 * @author zhouhao
 * @since 1.0
 */
public interface Measurement {

    MeasurementDefinition getDefinition();

    /**
     * 获取所有指标维度
     *
     * @return 维度
     */
    Flux<MeasurementDimension> getDimensions();

    /**
     * 获取指定ID的维度
     *
     * @param id 维度定义ID
     * @return 指定的维度, 不存在则返回 {@link Mono#empty()}
     */
    Mono<MeasurementDimension> getDimension(String id);

}
