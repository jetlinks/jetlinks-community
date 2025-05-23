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
package org.jetlinks.community.datasource;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.cache.ReactiveCacheContainer;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 默认数据源管理器
 * <p>
 * 获取数据源时,如果数据源不存在.
 * 则尝试从{@link DataSourceConfigManager#getConfig(String, String)}获取配置,
 * 然后调用{@link DataSourceConfig#getTypeId()}对应的{@link DataSourceProvider#getType()}
 * 进行初始化{@link DataSourceProvider#createDataSource(DataSourceConfig)}.
 *
 * <p>
 * 通过实现{@link DataSourceProvider}并注入到Spring容器中,即可实现自定义数据源.
 *
 * <p>
 * 当数据源配置发生变化时,将自动重新加载数据源.
 *
 * @author zhouhao
 * @see DataSourceProvider
 * @see DataSource
 * @since 1.9
 */
@Slf4j
public class DefaultDataSourceManager implements DataSourceManager {

    private final Map<String, DataSourceProvider> providers = new ConcurrentHashMap<>();

    private final ReactiveCacheContainer<CacheKey, DataSource> cachedDataSources = ReactiveCacheContainer.create();

    private final DataSourceConfigManager dataSourceConfigManager;

    public DefaultDataSourceManager(DataSourceConfigManager configManager) {
        this.dataSourceConfigManager = configManager;
        this.dataSourceConfigManager
            .doOnConfigChanged((state, properties) -> {
                //禁用,则删除数据源
                if (state == DataSourceConfigManager.ConfigState.disabled) {
                    this.removeDataSource(properties.getTypeId(), properties.getId());
                } else {

                    if (cachedDataSources.containsKey(new CacheKey(properties.getTypeId(), properties.getId()))) {
                        //重新加载
                        return this
                            .reloadDataSource(properties.getTypeId(), properties.getId())
                            .then();
                    }

                }
                return Mono.empty();
            });
    }

    /**
     * 注册一个数据源提供商
     *
     * @param provider 数据源提供商
     * @see DataSourceProvider
     */
    public void register(DataSourceProvider provider) {
        log.debug("Register DataSource {} Provider {}", provider.getType().getId(), provider);
        providers.put(provider.getType().getId(), provider);
    }

    /**
     * 注册一个已经初始化的数据源
     *
     * @param dataSource 数据源
     * @see DataSource
     */
    public void register(DataSource dataSource) {
        log.debug("Register DataSource {} {}", dataSource.getType().getId(), dataSource);
        CacheKey key = new CacheKey(dataSource.getType().getId(), dataSource.getId());
        cachedDataSources.put(key, dataSource);
    }

    @Override
    public DataSourceProvider getProvider(String typeId) {
        DataSourceProvider dataSourceProvider = providers.get(typeId);
        if (dataSourceProvider == null) {
            throw new UnsupportedOperationException("不支持的数据源类型：" + typeId);
        }
        return dataSourceProvider;
    }

    @Override
    public Flux<DataSource> getDataSources(String typeId) {
        return cachedDataSources
            .values()
            .filter(dataSource -> Objects.equals(typeId, dataSource.getType().getId()));
    }

    @Override
    public List<DataSourceType> getSupportedType() {
        return providers
            .values()
            .stream()
            .map(DataSourceProvider::getType)
            .collect(Collectors.toList());
    }

    @Override
    public Mono<DataSource> getDataSource(DataSourceType type,
                                          String datasourceId) {
        return getDataSource(type.getId(), datasourceId);
    }

    @Override
    public Mono<DataSource> getDataSource(String typeId, String datasourceId) {
        return getOrCreateRef(typeId, datasourceId);
    }

    /**
     * 获取数据源引用缓存，如果没有则自动加载,如果数据源不存在,不会立即报错.
     * 在使用{@link DataSourceRef#getRef()}才会返回错误.
     *
     * @param typeId       数据源类型ID
     * @param datasourceId 数据源ID
     * @return 数据源引用
     */
    private Mono<DataSource> getOrCreateRef(String typeId, String datasourceId) {
        return cachedDataSources
            .computeIfAbsent(new CacheKey(typeId, datasourceId),
                             key -> loadDataSource(key.type, key.datasourceId));

    }

    public Mono<Tuple2<DataSourceConfig, DataSourceProvider>> loadConfigAndProvider(String typeId, String datasourceId) {
        return Mono
            .zip(
                dataSourceConfigManager.getConfig(typeId, datasourceId),
                Mono
                    .justOrEmpty(providers.get(typeId))
                    .switchIfEmpty(Mono.error(() -> new UnsupportedOperationException("unsupported datasource type " + typeId)))
            );

    }

    public Mono<DataSource> loadDataSource(String typeId, String datasourceId) {
        return this
            .loadConfigAndProvider(typeId, datasourceId)
            .flatMap(tp2 -> tp2.getT2().createDataSource(tp2.getT1()))
            .doOnNext(dataSource -> log
                .debug("load {} datasource [{}]", dataSource.getType().getId(), dataSource.getId()));

    }

    private void removeDataSource(String typeId, String datasourceId) {
        cachedDataSources.remove(new CacheKey(typeId, datasourceId));
    }

    private Mono<Void> validateDataSource(DataSourceProvider provider, DataSourceConfig config) {

        return provider
            .createDataSource(config)
            .flatMap(dataSource -> dataSource
                .state()
                .doOnNext(DataSourceState::validate)
                //销毁测试数据源
                .doAfterTerminate(dataSource::dispose)
                .then()
            );
    }

    private Mono<DataSource> reloadDataSource(String typeId, String datasourceId) {


        return this
            .loadConfigAndProvider(typeId, datasourceId)
            //先校验一下
            .flatMap(tp2 -> this
                .validateDataSource(tp2.getT2(), tp2.getT1())
                .thenReturn(tp2))
            .flatMap(tp2 -> cachedDataSources
                .compute(
                    new CacheKey(typeId, datasourceId),
                    (key, old) -> {
                        if (old != null) {
                            return tp2.getT2().reload(old, tp2.getT1());
                        }
                        return tp2.getT2().createDataSource(tp2.getT1());

                    }
                ))
            .doOnError(err -> log.error("reload {} datasource [{}] error ", typeId, datasourceId, err));

    }

    @AllArgsConstructor
    @EqualsAndHashCode
    static class CacheKey {
        private final String type;
        private final String datasourceId;
    }

    static class DataSourceRef implements Disposable {

        @Getter
        private volatile Mono<DataSource> ref;

        private boolean disposed = false;

        public DataSourceRef(Mono<DataSource> ref) {
            this.ref = ref;
        }

        @Override
        public void dispose() {
            ref = Mono.empty();
            disposed = true;
        }

        @Override
        @Generated
        public boolean isDisposed() {
            return disposed;
        }
    }

}
