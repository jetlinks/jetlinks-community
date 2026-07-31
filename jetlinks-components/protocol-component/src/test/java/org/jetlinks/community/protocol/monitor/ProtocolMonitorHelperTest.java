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
package org.jetlinks.community.protocol.monitor;

import io.micrometer.context.ThreadLocalAccessor;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.monitor.Monitor;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.supports.event.InternalEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProtocolMonitorHelperTest {

    @AfterEach
    void resetCurrentMonitor() {
        ProtocolMonitorHelper.resetCurrentMonitor();
    }

    @Test
    void shouldOverrideProxyMonitorAndRestorePreviousMonitor() {
        EventBus eventBus = new InternalEventBus();
        ProtocolMonitorHelper helper = new ProtocolMonitorHelper(eventBus);
        Monitor proxy = helper.createMonitor("default-protocol");

        Monitor outer = mock(Monitor.class);
        Monitor current = mock(Monitor.class);
        Logger currentLogger = mock(Logger.class);
        when(current.logger()).thenReturn(currentLogger);

        ProtocolMonitorHelper.executeWith(outer, () -> {
            assertSame(outer, ProtocolMonitorHelper.getCurrentMonitor());
            Logger actual = ProtocolMonitorHelper.executeWith(current, proxy::logger);
            assertSame(currentLogger, actual);
            assertSame(outer, ProtocolMonitorHelper.getCurrentMonitor());
            return null;
        });

        assertNull(ProtocolMonitorHelper.getCurrentMonitor());
    }

    @Test
    void shouldPropagateAndRestoreMonitorForReactiveExecution() {
        Monitor current = mock(Monitor.class);

        Mono<Monitor> mono = Mono.defer(
            () -> Mono.just(ProtocolMonitorHelper.getCurrentMonitor())
        );
        StepVerifier
            .create(ProtocolMonitorHelper.executeWithMono(current, mono))
            .expectNext(current)
            .verifyComplete();

        Flux<Monitor> flux = Flux.defer(
            () -> Flux.just(
                ProtocolMonitorHelper.getCurrentMonitor(),
                ProtocolMonitorHelper.getCurrentMonitor()
            )
        );
        StepVerifier
            .create(ProtocolMonitorHelper.executeWithFlux(current, flux))
            .expectNext(current, current)
            .verifyComplete();

        assertNull(ProtocolMonitorHelper.getCurrentMonitor());
    }

    @Test
    void shouldRegisterThreadLocalAccessor() {
        boolean registered = ServiceLoader
            .load(ThreadLocalAccessor.class)
            .stream()
            .anyMatch(provider -> provider.type() == ProtocolMonitorThreadLocalAccessor.class);

        assertTrue(registered);
    }
}
