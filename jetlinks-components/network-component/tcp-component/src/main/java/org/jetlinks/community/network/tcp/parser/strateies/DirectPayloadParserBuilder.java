package org.jetlinks.community.network.tcp.parser.strateies;

import lombok.SneakyThrows;
import org.jetlinks.community.network.tcp.parser.DirectRecordParser;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.community.ValueObject;

import java.util.function.Supplier;

public class DirectPayloadParserBuilder implements PayloadParserBuilderStrategy {

    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DIRECT;
    }

    @Override
    @SneakyThrows
    public Supplier<PayloadParser> buildLazy(ValueObject config) {
        return DirectRecordParser::new;
    }
}
