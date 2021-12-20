package org.jetlinks.community.device.web.request;

import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.reactivestreams.Publisher;

import javax.annotation.Nonnull;

public class TestDeviceMessageCodec implements DeviceMessageCodec {

    private Transport transport;

    public TestDeviceMessageCodec(Transport transport) {
        this.transport = transport;
    }

    public TestDeviceMessageCodec() {
        this(DefaultTransport.MQTT);
    }

    @Override
    public Transport getSupportTransport() {
        return null;
    }

    @Nonnull
    @Override
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        FromDeviceMessageContext fromDeviceMessageContext = (FromDeviceMessageContext) context;
        fromDeviceMessageContext.getSession();
        return null;
    }

    @Nonnull
    @Override
    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        context.getMessage();
        context.getDevice();
        return null;
    }
}
