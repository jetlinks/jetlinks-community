package org.jetlinks.community.codec;

import io.netty.util.concurrent.FastThreadLocal;
import lombok.SneakyThrows;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;

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


}
