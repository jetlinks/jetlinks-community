package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringEscapeUtils;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

/**
 * 以分隔符读取数据包
 *
 * @author zhouhao
 * @since 1.0
 */
public class DelimitedPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DELIMITED;
    }

    @Override
    @SneakyThrows
    protected Supplier<RecordParser> createParser(ValueObject config) {

        String delimited = config
            .getString("delimited")
            .map(String::trim)
            .orElseThrow(() -> new IllegalArgumentException("delimited can not be null"));

        if (delimited.startsWith("0x")) {

            byte[] hex = Hex.decodeHex(delimited.substring(2));
            return () -> RecordParser
                .newDelimited(Buffer.buffer(hex));
        }

        return () -> RecordParser.newDelimited(StringEscapeUtils.unescapeJava(delimited));
    }


}
