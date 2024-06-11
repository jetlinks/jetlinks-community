package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.parsetools.RecordParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

/**
 * 固定长度解析器构造器,每次读取固定长度的数据包
 *
 * @author zhouhao
 * @since 1.0
 */
public class FixLengthPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.FIXED_LENGTH;
    }

    @Override
    protected Supplier<RecordParser> createParser(ValueObject config) {
        int size = config.getInt("size")
                         .orElseThrow(() -> new IllegalArgumentException("size can not be null"));

        return () -> RecordParser.newFixed(size);
    }


}
