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

import org.jetlinks.community.network.tcp.parser.strateies.DelimitedPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.strateies.FixLengthPayloadParserBuilder;
import org.jetlinks.community.network.tcp.parser.strateies.ScriptPayloadParserBuilder;
import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

/**
 * 解析器构造器策略，用于实现不同类型的解析器构造逻辑
 *
 * @author zhouhao
 * @since 1.0
 * @see FixLengthPayloadParserBuilder
 * @see DelimitedPayloadParserBuilder
 * @see ScriptPayloadParserBuilder
 */
public interface PayloadParserBuilderStrategy {
    /**
     * @return 解析器类型
     */
    PayloadParserType getType();

    /**
     * 构造解析器
     *
     * @param config 配置信息
     * @return 解析器
     */
    Supplier<PayloadParser> buildLazy(ValueObject config);

   default PayloadParser build(ValueObject config){
       return buildLazy(config).get();
   }
}
