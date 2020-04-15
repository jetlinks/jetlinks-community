package org.jetlinks.community.standalone.configuration.fst;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

@AllArgsConstructor
public class FstSerializationRedisSerializer implements RedisSerializer<Object> {

    private final FastThreadLocal<FSTConfiguration> configuration;

    public FstSerializationRedisSerializer(Supplier<FSTConfiguration> supplier) {

        this(new FastThreadLocal<FSTConfiguration>() {
            @Override
            protected FSTConfiguration initialValue() {
                return supplier.get();
            }
        });
    }

    @Override
    @SneakyThrows
    public byte[] serialize(Object o) throws SerializationException {
        ByteArrayOutputStream arr = new ByteArrayOutputStream(1024);
        try (FSTObjectOutput output = configuration.get().getObjectOutput(arr)) {
            output.writeObject(o);
        }
        return arr.toByteArray();
    }

    @Override
    @SneakyThrows
    public Object deserialize(byte[] bytes) throws SerializationException {

        try (FSTObjectInput input = configuration.get().getObjectInput(bytes)) {
            return input.readObject();
        }
    }
}
