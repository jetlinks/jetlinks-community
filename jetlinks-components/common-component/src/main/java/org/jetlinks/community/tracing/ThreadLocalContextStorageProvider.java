/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.tracing;

import io.micrometer.context.ThreadLocalAccessor;
import io.netty.util.concurrent.FastThreadLocal;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.context.ContextStorageProvider;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Bridges OpenTelemetry context storage with Micrometer context propagation.
 *
 * <p>Both SPIs operate on the same {@link FastThreadLocal}, allowing Reactor to restore the
 * OpenTelemetry {@link Context} before invoking an operator and to restore the previous context
 * after the signal. This provider only propagates existing contexts and never creates spans.</p>
 *
 * @see ContextStorageProvider
 * @see ThreadLocalAccessor
 * @since 2.12
 */
@Slf4j
public class ThreadLocalContextStorageProvider
    implements ContextStorageProvider, ThreadLocalAccessor<Context> {

    private static final FastThreadLocal<Context> CONTEXT_STORAGE = new FastThreadLocal<>();

    private static final ContextStorage STORAGE = new ThreadLocalContextStorage();

    @Override
    public ContextStorage get() {
        return STORAGE;
    }

    @Override
    @Nonnull
    public Object key() {
        return Context.class;
    }

    @Override
    @Nullable
    public Context getValue() {
        return CONTEXT_STORAGE.getIfExists();
    }

    @Override
    public void setValue(@Nonnull Context value) {
        CONTEXT_STORAGE.set(value);
    }

    @Override
    public void setValue() {
        CONTEXT_STORAGE.remove();
    }

    private static final class ThreadLocalContextStorage implements ContextStorage {

        @Override
        public Scope attach(Context toAttach) {
            if (toAttach == null) {
                return NoopScope.INSTANCE;
            }

            Context previous = current();
            if (toAttach == previous) {
                return NoopScope.INSTANCE;
            }

            CONTEXT_STORAGE.set(toAttach);
            return new RestoringScope(previous, toAttach);
        }

        @Override
        @Nullable
        public Context current() {
            return CONTEXT_STORAGE.getIfExists();
        }
    }

    private static final class RestoringScope implements Scope {

        @Nullable
        private final Context previous;

        private final Context attached;

        private boolean closed;

        private RestoringScope(@Nullable Context previous, Context attached) {
            this.previous = previous;
            this.attached = attached;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            // Only the matching scope may restore the context; out-of-order close would corrupt
            // a nested span and must leave the active context untouched.
            if (CONTEXT_STORAGE.getIfExists() != attached) {
                log.warn("Trying to close a scope which does not represent the current context. Ignoring the call.");
                return;
            }
            closed = true;
            if (previous == null) {
                CONTEXT_STORAGE.remove();
            } else {
                CONTEXT_STORAGE.set(previous);
            }
        }
    }

    private enum NoopScope implements Scope {
        INSTANCE;

        @Override
        public void close() {
        }
    }
}
