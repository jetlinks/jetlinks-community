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

import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.CommandHandler;
import org.jetlinks.core.monitor.Monitor;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * 数据源提供商,用于提供对数据源的支持.
 *
 * @author zhouhao
 * @see DataSource
 * @since 1.10
 */
public interface DataSourceProvider {

    /**
     * @return 数据源类型
     */
    @Nonnull
    DataSourceType getType();

    /**
     * 根据数据源配置来创建数据源
     *
     * @param properties 数据源配置
     * @return 数据源
     */
    @Nonnull
    Mono<DataSource> createDataSource(@Nonnull DataSourceConfig properties);

    /**
     * 使用新的配置来重新加载数据源
     *
     * @param dataSource 数据源
     * @param properties 配置
     * @return 重新加载后的数据源
     */
    @Nonnull
    Mono<DataSource> reload(@Nonnull DataSource dataSource,
                            @Nonnull DataSourceConfig properties);

    /**
     * 创建命令支持，用于提供针对某个数据源的命令支持.
     * <p>
     * 命令执行过程中请使用{@link CommandConfiguration#getMonitor()}进行日志打印以及链路追踪。
     *
     * @return 命令支持
     * @since 2.3
     */
    default Mono<CommandHandler<?, ?>> createCommandHandler(CommandConfiguration configuration) {
        return Mono.empty();
    }

    /**
     * 命令配置
     */
    interface CommandConfiguration {

        /**
         * 获取命令ID
         *
         * @return 命令ID
         * @see Command#getCommandId()
         */
        String getCommandId();

        /**
         * 获取命令名称
         *
         * @return 命令名称
         */
        String getCommandName();

        /**
         * 获取命令配置信息
         *
         * @return 配置信息
         */
        Map<String, Object> getConfiguration();

        /**
         * 获取数据源
         *
         * @return 数据源
         * @see DataSource#isWrapperFor(Class)
         * @see DataSource#unwrap(Class)
         */
        Mono<DataSource> getDataSource();

        /**
         * 获取监控器,用于日志打印,链路追踪等
         *
         * @return 监控器
         */
        Monitor getMonitor();
    }
}
