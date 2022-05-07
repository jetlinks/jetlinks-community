package org.jetlinks.community.configure.cluster;

import io.netty.util.concurrent.FastThreadLocal;
import io.scalecube.cluster.transport.api.Message;
import io.scalecube.cluster.transport.api.MessageCodec;
import lombok.AllArgsConstructor;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

@AllArgsConstructor
public class FSTMessageCodec implements MessageCodec {
    private final FastThreadLocal<FSTConfiguration> configuration;

    public FSTMessageCodec(Supplier<FSTConfiguration> supplier) {

        this(new FastThreadLocal<FSTConfiguration>() {
            @Override
            protected FSTConfiguration initialValue() {
                return supplier.get();
            }
        });
    }

    @Override
    public Message deserialize(InputStream stream) throws Exception {
        Message message = Message.builder().build();
        try (FSTObjectInput input = configuration.get().getObjectInput(stream)) {
            message.readExternal(input);
        }
        return message;
    }

    @Override
    public void serialize(Message message, OutputStream stream) throws Exception {
        try (FSTObjectOutput output = configuration.get().getObjectOutput(stream)) {
            message.writeExternal(output);
        }
    }
}
