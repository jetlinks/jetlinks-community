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
import org.jetlinks.core.monitor.Monitor;

import javax.annotation.Nonnull;

/**
 * 在 Micrometer 响应式上下文与协议执行线程之间传递当前设备 {@link Monitor}。
 *
 * <p>本类由 {@link java.util.ServiceLoader} 注册到 context propagation，保持无状态；
 * 仅负责恢复和清理 {@link ProtocolMonitorHelper} 的线程绑定，不创建或采集 Monitor。</p>
 *
 * @author zhangji
 * @see ProtocolMonitorHelper#executeWithMono(Monitor, reactor.core.publisher.Mono)
 * @see ProtocolMonitorHelper#executeWithFlux(Monitor, reactor.core.publisher.Flux)
 * @since 2.12
 */
public class ProtocolMonitorThreadLocalAccessor implements ThreadLocalAccessor<Monitor> {

    static final Object KEY = Monitor.class;

    @Override
    @Nonnull
    public Object key() {
        return KEY;
    }

    @Override
    public Monitor getValue() {
        return ProtocolMonitorHelper.getCurrentMonitor();
    }

    @Override
    public void setValue() {
        ProtocolMonitorHelper.resetCurrentMonitor();
    }

    @Override
    public void setValue(@Nonnull Monitor monitor) {
        ProtocolMonitorHelper.makeCurrentMonitor(monitor);
    }
}
