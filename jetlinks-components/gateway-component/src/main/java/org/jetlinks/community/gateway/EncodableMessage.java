package org.jetlinks.community.gateway;

import org.jetlinks.core.message.codec.EncodedMessage;

public interface EncodableMessage extends EncodedMessage {

    Object getNativePayload();

    static EncodableMessage of(Object object) {
        return new JsonEncodedMessage(object);
    }
}
