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

import lombok.Generated;
import lombok.Getter;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.core.command.AbstractCommandSupport;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public abstract class AbstractDataSource<C> extends AbstractCommandSupport implements DataSource {

    private final String id;

    private C config;

    private volatile boolean disposed;

    public AbstractDataSource(String id,
                              C config) {
        this.id = id;
        this.config = config;
    }

    @Override
    @Generated
    public final String getId() {
        return id;
    }

    @Override
    public abstract DataSourceType getType();

    @Override
    public final void dispose() {
        disposed = true;
        doOnDispose();
    }

    @Override
    public final boolean isDisposed() {
        return disposed;
    }

    @Generated
    public final C getConfig() {
        return config;
    }

    @SuppressWarnings("all")
    public C copyConfig() {
        return (C) FastBeanCopier.copy(config, config.getClass());
    }

    public final void setConfig(C config) {
        C old = this.config;
        this.config = config;
        handleSetConfig(old, config);
    }

    @Override
    public final Mono<DataSourceState> state() {
        if (isDisposed()) {
            return Mono.just(DataSourceState.stopped);
        }
        return this.checkState();
    }

    protected Mono<DataSourceState> checkState() {
        return Mono.just(DataSourceState.ok);
    }



    protected void handleSetConfig(C oldConfig, C newConfig) {


    }

    protected void doOnDispose() {

    }

    @Override
    protected <R> R executeUndefinedCommand(@Nonnull org.jetlinks.core.command.Command<R> command) {
        return super.executeUndefinedCommand(command);
    }

}
