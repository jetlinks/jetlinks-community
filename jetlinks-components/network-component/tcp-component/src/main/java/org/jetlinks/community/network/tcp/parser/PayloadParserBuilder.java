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

import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

/**
 * 解析器构造器，用于根据解析器类型和配置信息构造对应的解析器
 *
 * @author zhouhao
 * @since 1.0
 */
public interface PayloadParserBuilder {

    /**
     * 构造解析器
     *
     * @param type          解析器类型
     * @param configuration 配置信息
     * @return 解析器
     */
    Supplier<PayloadParser> build(PayloadParserType type, ValueObject configuration);

}
