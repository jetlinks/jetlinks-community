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
package org.jetlinks.community.datasource.command;

import lombok.AllArgsConstructor;
import org.jetlinks.core.Lazy;
import org.jetlinks.core.command.*;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.logger.Slf4jLogger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.SimpleTracer;
import org.jetlinks.core.monitor.tracer.Tracer;
import org.jetlinks.community.command.CommandSupportManagerProvider;
import org.jetlinks.community.datasource.DataSource;
import org.jetlinks.community.datasource.DataSourceConstants;
import org.jetlinks.community.datasource.DataSourceManager;
import org.jetlinks.community.datasource.DataSourceProvider;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
public abstract class DataSourceCommandSupportManager {

    public static final String defaultSupportId = "@@default";

    protected final DataSourceManager dataSourceManager;

    private final Map<String, DataSourceCommandSupportProvider> supports = new ConcurrentHashMap<>();

    protected Mono<Void> registerCommand(DataSourceCommandConfig config) {
        return supports
            .computeIfAbsent(
                config.getDatasourceId(),
                datasourceId -> new DataSourceCommandSupportProvider(this, datasourceId))
            .register(config);
    }

    protected Mono<Void> unregisterCommand(DataSourceCommandConfig config) {
        return Mono.fromRunnable(() -> {
            supports.compute(
                config.getDatasourceId(),
                (datasourceId,
                 provider) -> {
                    if (provider != null) {
                        provider.unregister(config);
                        if (provider.isEmpty()) {
                            provider.dispose();
                            return null;
                        }
                    }
                    return null;
                });
        });
    }

    protected abstract Mono<CommandSupportManagerProvider.CommandSupportInfo> getCommandSupportInfo(String datasourceId, String supportId);

    static class DataSourceCommandSupportProvider
        implements CommandSupportManagerProvider {

        private final DataSourceCommandSupportManager parent;
        private final String datasourceId;
        private final Disposable.Composite disposable = Disposables.composite();
        private final Map<String, DataSourceCommandSupport> commandSupports = new ConcurrentHashMap<>();

        DataSourceCommandSupportProvider(DataSourceCommandSupportManager parent,
                                         String datasourceId) {
            this.datasourceId = datasourceId;
            this.parent = parent;
            this.disposable.add(
                CommandSupportManagerProvider
                    .supports
                    .register(this.getProvider(), this)
            );
        }

        void dispose() {
            this.disposable.dispose();
        }

        boolean isEmpty() {
            return commandSupports.isEmpty();
        }

        //注册命令
        public Mono<Void> register(DataSourceCommandConfig config) {
            return parent
                .dataSourceManager
                .getProvider(config.getDatasourceType())
                .createCommandHandler(new CommandConfigurationImpl(config, parent.dataSourceManager))
                .doOnNext(handler -> registerHandler(config, handler))
                .then();
        }


        private void registerHandler(DataSourceCommandConfig config, CommandHandler<?, ?> handler) {
            String supportId = StringUtils.hasText(config.getSupportId()) ? config.getSupportId() : defaultSupportId;

            commandSupports
                .computeIfAbsent(supportId,
                                 id -> new DataSourceCommandSupport())
                .registerHandler(config.getCommandId(),
                                 handler);
        }

        //注销命令
        public void unregister(DataSourceCommandConfig config) {
            String supportId = StringUtils.hasText(config.getSupportId()) ? config.getSupportId() : defaultSupportId;
            commandSupports.compute(supportId, (id, support) -> {
                if (support != null) {
                    support.unregister(config.getCommandId());
                    if (support.isEmpty()) {
                        return null;
                    }
                }
                return null;
            });
        }

        @Override
        public String getProvider() {
            return DataSourceConstants.Commands.createCommandProvider(datasourceId);
        }

        @Override
        public Mono<? extends CommandSupport> getCommandSupport(String id, Map<String, Object> options) {
            if (id == null || Objects.equals(getProvider(), id)) {
                id = defaultSupportId;
            }

            return Mono.justOrEmpty(commandSupports.get(id));
        }

        @Override
        public Flux<CommandSupportInfo> getSupportInfo() {
            return Flux
                .fromIterable(commandSupports.entrySet())
                .flatMap(e -> {
                    String id = e.getKey();
                    if (Objects.equals(id, defaultSupportId)) {
                        id = null;
                    }
                    return parent.getCommandSupportInfo(datasourceId, id);
                }, 8);
        }

        static class DataSourceCommandSupport extends AbstractCommandSupport {
            @Override
            protected <C extends Command<R>, R> void registerHandler(String id, CommandHandler<C, R> handler) {
                super.registerHandler(id, handler);
            }

            void unregister(String commandId) {
                handlers.remove(commandId);
            }

            boolean isEmpty() {
                return handlers.isEmpty();
            }
        }
    }


    static class CommandConfigurationImpl extends Slf4jLogger
        implements DataSourceProvider.CommandConfiguration, Monitor {
        private final DataSourceCommandConfig config;
        private final DataSourceManager manager;
        private final Tracer tracer;

        CommandConfigurationImpl(DataSourceCommandConfig config,
                                 DataSourceManager manager) {
            super(LoggerFactory.getLogger("org.jetlinks.community.datasource." + config.getDatasourceType()));
            this.config = config;
            this.manager = manager;
            // TODO 企业版支持 链路追踪。
            this.tracer = Tracer.noop();
        }

        @Override
        public String getCommandId() {
            return config.getCommandId();
        }

        @Override
        public String getCommandName() {
            return config.getCommandName();
        }

        @Override
        public Map<String, Object> getConfiguration() {
            return config.getConfiguration();
        }

        @Override
        public Mono<DataSource> getDataSource() {
            return manager.getDataSource(config.getDatasourceType(), config.getDatasourceId());
        }

        @Override
        public Monitor getMonitor() {
            return this;
        }

        @Override
        public Logger logger() {
            return this;
        }

        @Override
        public Tracer tracer() {
            return tracer;
        }

        @Override
        public Metrics metrics() {
            return Metrics.noop();
        }

        @Override
        public void log(Level level, String message, Object... args) {
            // TODO 企业版支持 页面中实时查看日志。
            super.log(level, message, args);
        }
    }


}
