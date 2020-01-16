package org.jetlinks.community.network.tcp.parser.strateies;

import io.vertx.core.parsetools.RecordParser;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetlinks.community.network.tcp.parser.PayloadParserType;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;

public class DelimitedPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DELIMITED;
    }

    @Override
    protected RecordParser createParser(Values config) {

        return RecordParser.newDelimited(StringEscapeUtils.unescapeJava(config.getValue("delimited")
                .map(Value::asString)
                .orElseThrow(() -> new IllegalArgumentException("delimited can not be null"))));
    }


}
