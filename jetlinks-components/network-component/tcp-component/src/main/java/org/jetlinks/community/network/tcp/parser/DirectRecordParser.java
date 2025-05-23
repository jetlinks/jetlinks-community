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
package org.jetlinks.community.network.tcp.parser;

import io.vertx.core.buffer.Buffer;
import org.jetlinks.core.utils.Reactors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 不处理直接返回数据包
 *
 * @author zhouhao
 * @since 1.0
 */
public class DirectRecordParser implements PayloadParser {

    private final Sinks.Many<Buffer> sink = Reactors.createMany();

    @Override
    public void handle(Buffer buffer) {
        sink.emitNext(buffer, Reactors.emitFailureHandler());
    }

    @Override
    public Flux<Buffer> handlePayload() {
        return sink.asFlux();
    }

    @Override
    public void close() {
        sink.emitComplete(Reactors.emitFailureHandler());
    }
}
