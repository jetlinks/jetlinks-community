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

import org.jetlinks.core.command.CommandException;
import org.jetlinks.core.command.CommandSupport;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * 统一数据源接口定义,{@link DataSource#getType()}标识数据源类型.
 * <p>
 * 数据源统一管理,请勿手动调用{@link DataSource#dispose()}
 *
 * @author zhouhao
 * @since 1.10
 */
public interface DataSource extends CommandSupport, Disposable {

    /**
     * @return 数据源ID
     */
    String getId();

    /**
     * @return 数据源类型
     */
    DataSourceType getType();

    /**
     * 执行指令,具体指令有对应的数据源实现定义.
     *
     * @param command 指令
     * @param <R>     结果类型
     * @return void
     * @see UnsupportedOperationException
     */
    @Nonnull
    @Override
    default <R> R execute(@Nonnull org.jetlinks.core.command.Command<R> command) {
        throw new CommandException.NoStackTrace(this, command, "error.unsupported_command");
    }

    /**
     * 获取数据源状态
     *
     * @return 状态
     * @see DataSourceState
     */
    default Mono<DataSourceState> state() {
        return Mono.just(DataSourceState.ok);
    }

    /**
     * 判断数据源是为指定的类型
     *
     * @param target 类型
     * @return 是否为指定的类型
     */
    default boolean isWrapperFor(java.lang.Class<?> target) {
        return target.isInstance(this);
    }

    /**
     * 按指定类型拆箱数据源,返回对应的数据源。如果类型不一致,可能抛出{@link ClassCastException}
     *
     * @param target 目标类型
     * @param <T>    T
     * @return 数据源
     */
    default <T> T unwrap(Class<T> target) {
        return target.cast(this);
    }

}
