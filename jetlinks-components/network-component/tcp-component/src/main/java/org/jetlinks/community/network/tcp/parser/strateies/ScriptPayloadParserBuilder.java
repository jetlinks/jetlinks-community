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
