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
import org.jetlinks.community.network.tcp.parser.strateies.DelimitedPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.strateies.FixLengthPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.strateies.PipePayloadParser;
import reactor.core.publisher.Flux;

/**
 * 用于处理TCP粘拆包的解析器,通常一个客户端对应一个解析器.
 *
 * @author zhouhao
 * @see PipePayloadParser
 * @see FixLengthPayloadParserBuilder
 * @see DelimitedPayloadParserBuilder
 * @since 1.0
 */
public interface PayloadParser {

    /**
     * 处理一个数据包
     *
     * @param buffer 数据包
     */
    void handle(Buffer buffer);

    /**
     * 订阅完整的数据包流,每一个元素为一个完整的数据包
     *
     * @return 完整数据包流
     */
    Flux<Buffer> handlePayload();

    /**
     * 关闭以释放相关资源
     */
    void close();

    /**
     * 重置规则
     */
   default void reset(){}
}
