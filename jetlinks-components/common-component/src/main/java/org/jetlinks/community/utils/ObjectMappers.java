package org.jetlinks.community.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.unit.DataSize;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

public class ObjectMappers {

    public static final ObjectMapper JSON_MAPPER;
    public static final ObjectMapper CBOR_MAPPER;
    public static final ObjectMapper SMILE_MAPPER;

    static {
        JSON_MAPPER = Jackson2ObjectMapperBuilder
            .json()
            .build()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        ;
        {
            ObjectMapper cbor;

            try {
                cbor = Jackson2ObjectMapperBuilder
                    .cbor()
                    .build()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
            } catch (Throwable ignore) {
                cbor = null;
            }
            CBOR_MAPPER = cbor;
        }
        {
            ObjectMapper smile;

            try {

                smile = Jackson2ObjectMapperBuilder
                    .smile()
                    .build()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL);
            } catch (Throwable ignore) {
                smile = null;
            }
            SMILE_MAPPER = smile;
        }


    }


    @SneakyThrows
    public static String toJsonString(Object data) {
        return JSON_MAPPER.writeValueAsString(data);
    }

    @SneakyThrows
    public static byte[] toJsonBytes(Object data) {
        return JSON_MAPPER.writeValueAsBytes(data);
    }

    @SneakyThrows
    public static <T> T parseJson(byte[] data, Class<T> type) {
        return JSON_MAPPER.readValue(data, type);
    }

    @SneakyThrows
    public static <T> T parseJson(InputStream data, Class<T> type) {
        return JSON_MAPPER.readValue(data, type);
    }

    @SneakyThrows
    public static <T> T parseJson(String data, Class<T> type) {
        return JSON_MAPPER.readValue(data, type);
    }

    @SneakyThrows
    public static <T> List<T> parseJsonArray(InputStream data, Class<T> type) {
        return JSON_MAPPER.readerForListOf(type).readValue(data);
    }

    @SneakyThrows
    public static <T> List<T> parseJsonArray(byte[] data, Class<T> type) {
        return JSON_MAPPER.readerForListOf(type).readValue(data);
    }

    @SneakyThrows
    public static <T> T parseJsonArray(String data, Class<T> type) {
        return JSON_MAPPER.readerForListOf(type).readValue(data);
    }

    /**
     * 转换数据流为json对象流
     *
     * @param stream 数据流
     * @param type   json对象类型
     * @param <T>    json对象类型
     * @return json对象流
     */
    public static <T> Flux<T> parseJsonStream(Flux<DataBuffer> stream,
                                              Class<T> type) {
        return parseJsonStream(stream, type, JSON_MAPPER);
    }

    /**
     * 转换数据流为json对象流
     *
     * @param stream 数据流
     * @param type   json对象类型
     * @param mapper json转换器
     * @param <T>    json对象类型
     * @return json对象流
     */
    public static <T> Flux<T> parseJsonStream(Flux<DataBuffer> stream,
                                              Class<T> type,
                                              ObjectMapper mapper) {
        Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(mapper);
        decoder.setMaxInMemorySize((int) DataSize.ofMegabytes(8).toBytes());
        return decoder
            .decode(stream, ResolvableType.forType(type), null, null)
            .cast(type);
    }

    /**
     * 转换数据流为json字节流
     *
     * @param objectStream 数据流
     * @return json字节流
     */
    public static Flux<byte[]> toJsonStream(Flux<?> objectStream) {
        return toJsonStream(objectStream, JSON_MAPPER);
    }

    /**
     * 转换数据流为json字节流
     *
     * @param objectStream 数据流
     * @param mapper       json转换器
     * @return json字节流
     */
    public static Flux<byte[]> toJsonStream(Flux<?> objectStream, ObjectMapper mapper) {
        return Flux.create(sink -> {
            OutputStream stream = createStream(sink, 8096);

            JsonGenerator generator = createJsonGenerator(mapper, stream);
            try {
                generator.writeStartArray();
            } catch (IOException e) {
                sink.error(e);
                return;
            }
            @SuppressWarnings("all")
            Disposable writer = objectStream
                .publishOn(Schedulers.single(Schedulers.boundedElastic()))
                .subscribe(
                    next -> writeObject(generator, next),
                    sink::error,
                    () -> {
                        try {
                            generator.writeEndArray();
                        } catch (IOException e) {
                            sink.error(e);
                            return;
                        }
                        safeClose(generator);
                        sink.complete();
                    },
                    Context.of(sink.contextView()));

            sink.onDispose(() -> {
                safeClose(generator);
                writer.dispose();
            });
        });
    }

    private static OutputStream createStream(FluxSink<byte[]> sink, int bufferSize) {
        return new BufferedOutputStream(new OutputStream() {

            @Override
            public void write(@Nonnull byte[] b, int off, int len) {
                if (len == b.length) {
                    sink.next(b);
                } else {
                    sink.next(Arrays.copyOfRange(b, off, off + len));
                }
            }

            @Override
            public void write(@Nonnull byte[] b) {
                sink.next(b);
            }

            @Override
            public void write(int b) {
                sink.next(new byte[]{(byte) b});
            }
        }, bufferSize) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    sink.complete();
                }
            }
        };
    }

    @SneakyThrows
    private static JsonGenerator createJsonGenerator(ObjectMapper mapper, OutputStream stream) {
        return mapper.createGenerator(stream);
    }

    @SneakyThrows
    private static void writeObject(JsonGenerator generator, Object data) {
        generator.writePOJO(data);
    }

    private static void safeClose(JsonGenerator closeable) {
        if (closeable.isClosed()) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignore) {

        }
    }

}
