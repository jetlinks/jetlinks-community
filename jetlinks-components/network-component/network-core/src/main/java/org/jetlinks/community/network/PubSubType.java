package org.jetlinks.community.network;

import lombok.Getter;
import org.hswebframework.web.dict.EnumDict;

@Getter
public enum PubSubType implements EnumDict<String> {

    producer,
    consumer;

    @Override
    public String getValue() {
        return name();
    }

    @Override
    public String getText() {
        return name();
    }
}
