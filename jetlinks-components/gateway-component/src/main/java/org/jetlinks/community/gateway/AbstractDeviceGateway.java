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
package org.jetlinks.community.gateway;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.core.message.Message;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;

@Slf4j
public abstract class AbstractDeviceGateway implements DeviceGateway {
    private final static AtomicReferenceFieldUpdater<AbstractDeviceGateway, GatewayState>
        STATE = AtomicReferenceFieldUpdater.newUpdater(AbstractDeviceGateway.class, GatewayState.class, "state");

    private final String id;

    private final List<BiConsumer<GatewayState, GatewayState>> stateListener = new CopyOnWriteArrayList<>();

    private volatile GatewayState state = GatewayState.shutdown;

    protected final DeviceGatewayMonitor monitor;

    public AbstractDeviceGateway(String id) {
        this.id = id;
        this.monitor = GatewayMonitors.getDeviceGatewayMonitor(id);
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public Flux<Message> onMessage() {
        return Flux.empty();
    }

    @Override
    public final synchronized Mono<Void> startup() {
        if (state == GatewayState.paused) {
            changeState(GatewayState.started);
            return Mono.empty();
        }
        if (state == GatewayState.started || state == GatewayState.starting) {
            return Mono.empty();
        }
        changeState(GatewayState.starting);
        return this
            .doStartup()
            .doOnSuccess(ignore -> changeState(GatewayState.started));
    }

    @Override
    public final Mono<Void> pause() {
        changeState(GatewayState.paused);
        return Mono.empty();
    }

    @Override
    public final Mono<Void> shutdown() {
        GatewayState old = STATE.getAndSet(this, GatewayState.shutdown);

        if (old == GatewayState.shutdown) {
            return Mono.empty();
        }
        changeState(GatewayState.shutdown);
        return doShutdown();
    }

    protected abstract Mono<Void> doShutdown();

    protected abstract Mono<Void> doStartup();

    protected synchronized final void changeState(GatewayState target) {
        GatewayState old = STATE.getAndSet(this, target);
        if (target == old) {
            return;
        }
        for (BiConsumer<GatewayState, GatewayState> consumer : stateListener) {
            try {
                consumer.accept(old, this.state);
            } catch (Throwable error) {
                log.warn("fire gateway {} state listener error", getId(), error);
            }
        }
    }

    @Override
    public final GatewayState getState() {
        return state;
    }

    @Override
    public final void doOnStateChange(BiConsumer<GatewayState, GatewayState> listener) {
        stateListener.add(listener);
    }

    @Override
    public final void doOnShutdown(Disposable disposable) {
        DeviceGateway.super.doOnShutdown(disposable);
    }

    @Override
    public final boolean isAlive() {
        return state == GatewayState.started || state == GatewayState.starting;
    }
}
