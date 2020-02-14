package org.jetlinks.community.network.tcp.parser.strateies;

import lombok.SneakyThrows;
import org.jetlinks.core.Values;
import org.jetlinks.community.network.tcp.parser.DirectRecordParser;
import org.jetlinks.community.network.tcp.parser.PayloadParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserBuilderStrategy;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;

public class DirectPayloadParserBuilder implements PayloadParserBuilderStrategy {

    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DIRECT;
    }

    @Override
    @SneakyThrows
    public PayloadParser build(Values config) {
        return new DirectRecordParser();
    }
}
