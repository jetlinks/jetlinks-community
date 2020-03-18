package org.jetlinks.community.network.tcp.parser;

import org.jetlinks.community.ValueObject;
import org.jetlinks.core.Values;

public interface PayloadParserBuilder {

    PayloadParser build(PayloadParserType type, ValueObject configuration);

}
