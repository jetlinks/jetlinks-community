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
package org.jetlinks.community.protocol.local;

import lombok.AllArgsConstructor;
import lombok.Generated;
import org.jetlinks.community.protocol.CommandSupportServiceContext;
import org.jetlinks.community.protocol.CompositeServiceContext;
import org.jetlinks.community.protocol.monitor.ProtocolMonitorHelper;
import org.jetlinks.core.spi.ServiceContext;
import org.jetlinks.community.ValueObject;
import org.jetlinks.supports.protocol.management.ProtocolSupportDefinition;
import org.jetlinks.supports.protocol.management.ProtocolSupportLoaderProvider;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.FileNotFoundException;

@AllArgsConstructor
@Generated
public class LocalProtocolSupportLoader implements ProtocolSupportLoaderProvider {

    private final ServiceContext serviceContext;
    private final ProtocolMonitorHelper helper;

    @Override
    public String getProvider() {
        return "local";
    }

    @Override
    public Mono<LocalFileProtocolSupport> load(ProtocolSupportDefinition definition) {

        return Mono
            .fromCallable(() -> {
                ValueObject config = ValueObject.of(definition.getConfiguration());

                String location = config
                    .getString("location")
                    .orElseThrow(() -> new IllegalArgumentException("location cannot be null"));

                String provider = config
                    .get("provider")
                    .map(String::valueOf)
                    .map(String::trim)
                    .orElse(null);

                File file = new File(location);
                if (!file.exists()) {
                    throw new FileNotFoundException("文件" + file.getName() + "不存在");
                }

                LocalFileProtocolSupport support = new LocalFileProtocolSupport();
                String id = definition.getId();

                support.init(file, CompositeServiceContext.of(
                    helper.createMonitor(id),
                    deviceId -> helper.createMonitor(id, deviceId),
                    CommandSupportServiceContext.INSTANCE,
                    serviceContext
                ), provider);
                return support;
            })
            .subscribeOn(Schedulers.boundedElastic());
    }
}
