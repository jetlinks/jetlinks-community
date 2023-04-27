package org.jetlinks.community.device.message.transparent;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.OperationSource;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.DirectDeviceMessage;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.function.ThingFunctionInvokeMessage;
import org.jetlinks.core.message.property.ReadThingPropertyMessage;
import org.jetlinks.core.message.property.ReportPropertyMessage;
import org.jetlinks.core.message.property.WriteThingPropertyMessage;
import org.jetlinks.core.utils.TopicUtils;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class SimpleTransparentMessageCodec implements TransparentMessageCodec {

    @NonNull
    protected final Codec codec;

    public SimpleTransparentMessageCodec(@NonNull Codec codec) {
        this.codec = codec;
    }


    @Override
    public final Mono<DirectDeviceMessage> encode(DeviceMessage message) {

        return Mono.defer(() -> {

            EncodeContext context = new EncodeContext(message);

            codec.encode(context);

            if (context.payload != null) {
                DirectDeviceMessage msg = new DirectDeviceMessage();
                msg.setPayload(ByteBufUtil.getBytes(context.payload));
                //release
                ReferenceCountUtil.safeRelease(context.payload);

                msg.setMessageId(message.getMessageId());
                msg.setDeviceId(message.getDeviceId());
                if (null != message.getHeaders()) {
                    message.getHeaders().forEach(msg::addHeader);
                }
                context.headers.forEach(msg::addHeader);
                return Mono.just(msg);

            }
            return Mono.empty();
        });
    }

    @Override
    public Flux<DeviceMessage> decode(DirectDeviceMessage message) {

        return Mono
            .fromCallable(() -> codec.decode(new DecodeContext(message)))
            .flatMapMany(this::convert)
            .doOnNext(msg -> {
                String from = message.getMessageId();
                if (from == null) {
                    from = message.getHeader(PropertyConstants.uid).orElse(null);
                }
                if (from != null) {
                    msg.addHeader("decodeFrom", from);
                }
                msg.thingId(message.getThingType(), message.getThingId());
            });

    }

    @SuppressWarnings("all")
    protected Flux<DeviceMessage> convert(Object msg) {
        if (msg == null) {
            return Flux.empty();
        }
        if (msg instanceof DeviceMessage) {
            return Flux.just(((DeviceMessage) msg));
        }
        if (msg instanceof Map) {
            if (MapUtils.isEmpty(((Map) msg))) {
                return Flux.empty();
            }
            MessageType type = MessageType.of(((Map<String, Object>) msg)).orElse(MessageType.UNKNOWN);
            if (type == MessageType.UNKNOWN) {
                //返回map但是未设备未设备消息,则转为属性上报
                return Flux.just(new ReportPropertyMessage().properties(((Map) msg)));
            }
            return Mono
                .justOrEmpty(type.convert(((Map) msg)))
                .flux()
                .cast(DeviceMessage.class);
        }
        if (msg instanceof Collection) {
            return Flux
                .fromIterable(((Collection<?>) msg))
                .flatMap(this::convert);
        }
        if (msg instanceof Publisher) {
            return Flux
                .from(((Publisher<?>) msg))
                .flatMap(this::convert);
        }
        return Flux.error(new UnsupportedOperationException("unsupported data:" + msg));
    }

    public static class DecodeContext {
        final DirectDeviceMessage msg;
        final ByteBuf buffer;

        DecodeContext(DirectDeviceMessage msg) {
            this.msg = msg;
            this.buffer = msg.asByteBuf();
        }

        public long timestamp() {
            return msg.getTimestamp();
        }

        public ByteBuf payload() {
            return buffer;
        }

        public Object json() {
            return JSON.parse(buffer.array());
        }

        public Map<String, String> pathVars(String pattern, String path) {
            return TopicUtils.getPathVariables(pattern, path);
        }

        public String url() {
            return msg.getHeader("url")
                      .map(String::valueOf)
                      .orElse(null);
        }

        public String topic() {
            return msg.getHeader("topic")
                      .map(String::valueOf)
                      .orElse(null);
        }

        public DirectDeviceMessage message() {
            return msg;
        }

    }

    /**
     * <pre>{@code
     *
     * context
     * .whenReadProperty("temp",()->return "0x0122")
     * .whenFunction("func",args->{
     *
     * })
     *
     * }</pre>
     */
    public static class EncodeContext {

        private final DeviceMessage source;
        private ByteBuf payload;
        private final Map<String, Object> headers = new HashMap<>();

        public EncodeContext(DeviceMessage source) {
            this.source = source;
        }

        public DeviceMessage message() {
            return source;
        }

        public EncodeContext topic(String topic) {
            headers.put("topic", topic);
            return this;
        }

        public ByteBuf payload() {
            return payload == null ? payload = Unpooled.buffer() : payload;
        }

        public ByteBuf newBuffer() {
            return Unpooled.buffer();
        }

        @SneakyThrows
        public EncodeContext setPayload(String strOrHex, String charset) {
            if (strOrHex.startsWith("0x")) {
                payload().writeBytes(Hex.decodeHex(strOrHex.substring(2)));
            } else {
                payload().writeBytes(strOrHex.getBytes(charset));
            }
            return this;
        }

        @SneakyThrows
        public EncodeContext setPayload(String strOrHex) {
            setPayload(strOrHex, "utf-8");
            return this;
        }

        public EncodeContext setPayload(Object data) {

            if (data instanceof String) {
                setPayload(((String) data));
            }

            if (data instanceof byte[]) {
                payload().writeBytes(((byte[]) data));
            }

            if (data instanceof ByteBuf) {
                this.payload = ((ByteBuf) data);
            }
            //todo 更多类型?

            return this;
        }

        public EncodeContext whenFunction(String functionId, Function<Object, Object> supplier) {
            if (source instanceof ThingFunctionInvokeMessage) {
                ThingFunctionInvokeMessage<?> msg = ((ThingFunctionInvokeMessage<?>) source);
                if ("*".equals(msg.getFunctionId()) || Objects.equals(functionId, msg.getFunctionId())) {
                    setPayload(supplier.apply(msg.inputsToMap()));
                }
            }
            return this;
        }

        public EncodeContext whenWriteProperty(String property, Function<Object, Object> supplier) {
            if (source instanceof WriteThingPropertyMessage) {
                if ("*".equals(property)) {
                    setPayload(supplier.apply(((WriteThingPropertyMessage<?>) source).getProperties()));
                    return this;
                }
                Object value = ((WriteThingPropertyMessage<?>) source).getProperties().get(property);
                if (value != null) {
                    setPayload(supplier.apply(value));
                }
            }
            return this;
        }

        public EncodeContext whenReadProperties(Function<List<String>, Object> supplier) {
            if (source instanceof ReadThingPropertyMessage) {
                setPayload(supplier.apply(((ReadThingPropertyMessage<?>) source).getProperties()));
            }
            return this;
        }

        public EncodeContext whenReadProperty(String property, Supplier<Object> supplier) {
            if (source instanceof ReadThingPropertyMessage) {
                if ("*".equals(property) || ((ReadThingPropertyMessage<?>) source).getProperties().contains(property)) {
                    setPayload(supplier.get());
                }
            }
            return this;
        }
    }

    public interface Codec {
        Object decode(DecodeContext context);

        Object encode(EncodeContext context);
    }
}
