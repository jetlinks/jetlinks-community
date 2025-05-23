/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.FastThreadLocal;
import lombok.SneakyThrows;
import org.jetlinks.core.utils.SerializeUtils;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.util.Base64;
import java.util.function.Supplier;

public class Serializers {

    private static final ObjectSerializer JDK = new ObjectSerializer() {
        @Override
        @SneakyThrows
        public ObjectInput createInput(InputStream stream) {
            return new ObjectInputStream(stream);
        }

        @Override
        @SneakyThrows
        public ObjectOutput createOutput(OutputStream stream) {
            return new ObjectOutputStream(stream);
        }
    };

    private static final ObjectSerializer FST = new ObjectSerializer() {

        final FastThreadLocal<FSTConfiguration> conf =
            new FastThreadLocal<FSTConfiguration>() {
                @Override
                protected FSTConfiguration initialValue() {
                    FSTConfiguration configuration = FSTConfiguration.createDefaultConfiguration();
                    configuration.setForceSerializable(true);
                    configuration.setClassLoader(FST.getClass().getClassLoader());
                    return configuration;
                }
            };

        @Override
        @SneakyThrows
        public ObjectInput createInput(InputStream stream) {
            return conf.get().getObjectInput(stream);
        }

        @Override
        @SneakyThrows
        public ObjectOutput createOutput(OutputStream stream) {
            return conf.get().getObjectOutput(stream);
        }
    };

    private static final ObjectSerializer DEFAULT;

    static {
        DEFAULT = System.getProperty("jetlinks.object.serializer.type", "fst").equals("fst") ? FST : JDK;
    }

    public static ObjectSerializer jdk() {
        return JDK;
    }

    public static ObjectSerializer fst() {
        return FST;
    }


    public static ObjectSerializer getDefault() {
        return DEFAULT;
    }

    public static String serializeToBase64(Object source) {
        return Base64.getEncoder().encodeToString(serialize(source));
    }

    public static Object deserializeFromBase64(String base64) {
        return deserialize(Base64.getDecoder().decode(base64));
    }



    @SuppressWarnings("all")
    private static final FastThreadLocal<ByteArrayOutputStream>
        SHARED_STREAM = new FastThreadLocal<ByteArrayOutputStream>() {
        @Override
        protected ByteArrayOutputStream initialValue() {
            return new ByteArrayOutputStream();
        }
    };

    @SneakyThrows
    public static ByteBuf serializeExternal(Externalizable source) {
        ByteArrayOutputStream outputStream = SHARED_STREAM.get();
        if (outputStream.size() != 0) {
            outputStream = new ByteArrayOutputStream();
        }
        try (ObjectOutput output = getDefault().createOutput(outputStream)) {
            source.writeExternal(output);
            output.flush();
            return Unpooled.wrappedBuffer(outputStream.toByteArray());
        } finally {
            outputStream.reset();
        }
    }

    @SneakyThrows
    public static <T extends Externalizable> T deserializeExternal(ByteBuf buffer, Supplier<T> instance) {
        try (ObjectInput input = getDefault().createInput(new ByteBufInputStream(buffer, true))) {
            T data = instance.get();
            data.readExternal(input);
            return data;
        }
    }


    @SneakyThrows
    public static byte[] serialize(Object source) {
        ByteArrayOutputStream outputStream = SHARED_STREAM.get();
        if (outputStream.size() != 0) {
            outputStream = new ByteArrayOutputStream();
        }
        try (ObjectOutput output = getDefault().createOutput(outputStream)) {
            SerializeUtils.writeObject(source, output);
            output.flush();
            return outputStream.toByteArray();
        } finally {
            outputStream.reset();
        }

    }

    @SneakyThrows
    public static Object deserialize(byte[] data) {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        try (ObjectInput input = getDefault().createInput(stream)) {
            return SerializeUtils.readObject(input);
        }
    }

}
