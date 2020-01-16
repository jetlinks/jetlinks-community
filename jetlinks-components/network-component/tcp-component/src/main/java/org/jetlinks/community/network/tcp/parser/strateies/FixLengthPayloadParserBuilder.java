package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.parsetools.RecordParser;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;

public class FixLengthPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.FIXED_LENGTH;
    }

    @Override
    protected RecordParser createParser(Values config) {
        return RecordParser.newFixed(config.getValue("size")
                .map(Value::asInt)
                .orElseThrow(() -> new IllegalArgumentException("size can not be null")));
    }


}
