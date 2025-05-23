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
import lombok.SneakyThrows;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.network.tcp.parser.strateies.PipePayloadParser;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 长度字段粘拆包解析规则, 使用指定的字节数据来表示接下来的包长度.
 *
 * @author zhouhao
 * @since 1.0
 */
public class LengthFieldPayloadParserBuilder implements PayloadParserBuilderStrategy {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.LENGTH_FIELD;
    }

    @Override
    @SneakyThrows
    public Supplier<PayloadParser> buildLazy(ValueObject config) {
        //偏移量
        int offset = config.getInt("offset")
                           .orElse(0);

        //包长度
        int len = config.getInt("length")
                        .orElseGet(() -> config
                            .getInt("to")
                            .orElse(4) - offset);

        //是否为小端模式
        boolean le = config.getBoolean("little")
                           .orElse(false);

        int initLength = offset + len;

        Function<Buffer, Integer> lengthParser;
        switch (len) {
            case 1:
                lengthParser = buffer -> (int) buffer.getUnsignedByte(offset);
                break;
            case 2:
                lengthParser =
                    le ? buffer -> buffer.getUnsignedShortLE(offset)
                        : buffer -> buffer.getUnsignedShort(offset);
                break;
            case 3:
                lengthParser =
                    le ? buffer -> buffer.getUnsignedMediumLE(offset)
                        : buffer -> (int) buffer.getUnsignedMedium(offset);
                break;
            case 4:
                lengthParser =
                    le ? buffer -> buffer.getIntLE(offset)
                        : buffer -> (int) buffer.getInt(offset);
                break;
            case 8:
                lengthParser =
                    le ? buffer -> (int) buffer.getLongLE(offset)
                        : buffer -> (int) buffer.getLong(offset);
                break;
            default:
                throw new IllegalArgumentException("illegal length:" + len);
        }


        return () -> new PipePayloadParser()
            //先读取初始长度
            .fixed(initLength)
            .handler((buffer, parser) -> {
                //获取长度字段，然后读取接下来的长度
                int next = lengthParser.apply(buffer);
                parser.result(buffer)
                      .fixed(next);
            })
            .handler((buffer, parser) -> parser
                .result(buffer)
                .complete());
    }
}
