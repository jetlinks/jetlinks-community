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
package org.jetlinks.community.network.tcp.parser.strateies;

import lombok.SneakyThrows;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.script.CompiledScript;
import org.jetlinks.community.script.Script;
import org.jetlinks.community.script.Scripts;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 利用脚本引擎和{@link PipePayloadParser}来描述解析器
 * <p>
 * 在脚本中可以使用内置变量: parser,对应类型为:{@link PipePayloadParser},如:
 *
 * <pre>{@code
 * parser.fixed(4)
 *       .handler(function(buffer,parser){
 *             var len = buffer.getInt(0);
 *             parser.fixed(len).result(buffer);
 *         })
 *       .handler(function(buffer,parser){
 *             parser.result(buffer)
 *                    .complete();
 *         });
 * }</pre>
 *
 * @author zhouhao
 * @since 1.0
 */
public class ScriptPayloadParserBuilder implements PayloadParserBuilderStrategy {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.SCRIPT;
    }

    @Override
    @SneakyThrows
    public Supplier<PayloadParser> buildLazy(ValueObject config) {
        String script = config.getString("script")
                              .orElseThrow(() -> new IllegalArgumentException("script不能为空"));
        String lang = config.getString("lang")
                            .orElse("js");

        CompiledScript compiledScript = Scripts
            .getFactory(lang)
            .compile(Script.of("tcp-network-payload-parser", script));

        return () -> {
            PipePayloadParser parser = new PipePayloadParser();

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("parser", parser);

            compiledScript.call(ctx);
            return parser;
        };
    }
}
