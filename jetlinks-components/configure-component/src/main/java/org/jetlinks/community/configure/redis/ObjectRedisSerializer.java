package org.jetlinks.community.configure.redis;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.core.utils.SerializeUtils;
import org.jetlinks.community.codec.Serializers;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@Slf4j
@AllArgsConstructor
public class ObjectRedisSerializer implements RedisSerializer<Object> {

    static final FastThreadLocal<ByteArrayOutputStream> STREAM_LOCAL = new FastThreadLocal<ByteArrayOutputStream>() {
        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream(1024) {
                @Override
                public void close() {
                    reset();
                }
            };
        }
    };

    @Override
    @SneakyThrows
    public byte[] serialize(Object o) throws SerializationException {
        if (o == null) {
            return null;
        }
        ByteArrayOutputStream arr = STREAM_LOCAL.get();
        try (ObjectOutput output = Serializers.getDefault().createOutput(arr)) {

            SerializeUtils.writeObject(o, output);
            output.flush();

            return arr.toByteArray();
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @SneakyThrows
    public Object deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null) {
            return null;
        }
        try (ObjectInput input = Serializers
            .getDefault()
            .createInput(new ByteArrayInputStream(bytes))) {
            return SerializeUtils.readObject(input);
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }
}
