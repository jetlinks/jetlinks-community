package org.jetlinks.community.network.tcp.parser.strateies;

import lombok.SneakyThrows;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.ValueObject;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(lang);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的脚本:" + lang);
        }

        String id = DigestUtils.md5Hex(script);
        if (!engine.compiled(id)) {
            engine.compile(id, script);
        }
        return () -> {
            PipePayloadParser parser = new PipePayloadParser();

            Map<String, Object> ctx = new HashMap<>();
            ctx.put("parser", parser);
            try {
                engine.execute(id, ctx).getIfSuccess();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            return parser;
        };

    }
}
