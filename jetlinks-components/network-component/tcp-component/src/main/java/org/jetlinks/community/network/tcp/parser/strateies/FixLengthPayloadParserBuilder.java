package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.parsetools.RecordParser;
import org.jetlinks.community.ValueObject;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;

public class FixLengthPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.FIXED_LENGTH;
    }

    @Override
    protected RecordParser createParser(ValueObject config) {
        return RecordParser.newFixed(config.getInt("size")
                .orElseThrow(() -> new IllegalArgumentException("size can not be null")));
    }


}
