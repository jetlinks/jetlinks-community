package org.jetlinks.community.network.tcp.parser;

import org.jetlinks.core.Values;

public interface PayloadParserBuilder {

    PayloadParser build(PayloadParserType type, Values configuration);

}
