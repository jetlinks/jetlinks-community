package org.jetlinks.community.datasource;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.function.BiFunction;

/**
 * 数据源配置管理器,统一管理数据源配置
 *
 * @author zhouhao
 * @since 1.10
 */
public interface DataSourceConfigManager {

    /**
     * 根据类型ID和数据源ID获取配置
     *
     * @param typeId       类型ID
     * @param datasourceId 数据源ID
     * @return 配置信息
     */
    Mono<DataSourceConfig> getConfig(String typeId, String datasourceId);

    /**
     * 监听配置变化，当有配置变化后将调用回调参数
     *
     * @param callback 回调参数
     */
    Disposable doOnConfigChanged(BiFunction<ConfigState, DataSourceConfig,Mono<Void>> callback);

    enum ConfigState{
        normal,
        disabled
    }
}
