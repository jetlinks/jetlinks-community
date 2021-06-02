package org.jetlinks.community.network.tcp.parser;

import org.jetlinks.community.ValueObject;

public interface PayloadParserBuilder {

    PayloadParser build(PayloadParserType type, ValueObject configuration);

}
