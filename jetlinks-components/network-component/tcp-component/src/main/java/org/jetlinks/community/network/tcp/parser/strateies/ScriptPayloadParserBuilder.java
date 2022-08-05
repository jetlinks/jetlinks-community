package org.jetlinks.community.network.tcp.parser.strateies;

import lombok.SneakyThrows;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;

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
                            .orElseThrow(() -> new IllegalArgumentException("lang不能为空"));

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(lang);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的脚本:" + lang);
        }
        String id = DigestUtils.md5Hex(script);
        if (!engine.compiled(id)) {
            engine.compile(id, script);
        }
        doCreateParser(id,engine);
        return ()-> doCreateParser(id, engine);
    }

    @SneakyThrows
    private PipePayloadParser doCreateParser(String id,DynamicScriptEngine engine){
        PipePayloadParser parser = new PipePayloadParser();
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("parser", parser);
        engine.execute(id, ctx).getIfSuccess();
        return parser;
    }
}
