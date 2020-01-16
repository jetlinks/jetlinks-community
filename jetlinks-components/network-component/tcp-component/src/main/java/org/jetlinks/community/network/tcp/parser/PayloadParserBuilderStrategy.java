package org.jetlinks.community.network.tcp.parser;

import org.jetlinks.core.Values;

public interface PayloadParserBuilderStrategy {
    PayloadParserType getType();

    PayloadParser build(Values config);
}
