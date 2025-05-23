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
package org.jetlinks.community.plugin.impl;

import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.core.command.Command;
import org.jetlinks.core.command.ProxyCommandSupportAdapter;
import org.jetlinks.plugin.core.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Objects;

class PluginDriverWrapper extends ProxyCommandSupportAdapter implements PluginDriver {
    private final PluginDriver target;

    public PluginDriverWrapper(PluginDriver target) {
        super(target);
        this.target = target;
    }

    @Nonnull
    @Override
    public Description getDescription() {
        return target.getDescription();
    }

    @Nonnull
    @Override
    public PluginType getType() {
        return target.getType();
    }

    @Nonnull
    @Override
    public Mono<? extends Plugin> createPlugin(@Nonnull String pluginId, @Nonnull PluginContext context) {
        try {
            return target
                .createPlugin(pluginId, context)
                .onErrorResume(
                    err -> !(err instanceof BusinessException),
                    error -> Mono.error(new I18nSupportException.NoStackTrace(
                        "error.create_plugin_error",
                        error,
                        Objects.toString(error.getLocalizedMessage(), error.getClass().getSimpleName()))));

        } catch (Throwable error) {
            return Mono.error(new I18nSupportException.NoStackTrace(
                "error.create_plugin_error",
                error,
                Objects.toString(error.getLocalizedMessage(), error.getClass().getSimpleName())));
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> type) {
        return target.isWrapperFor(type);
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return target.unwrap(type);
    }

    @Override
    public Flux<DataBuffer> getResource(String name) {
        return target.getResource(name);
    }
}
