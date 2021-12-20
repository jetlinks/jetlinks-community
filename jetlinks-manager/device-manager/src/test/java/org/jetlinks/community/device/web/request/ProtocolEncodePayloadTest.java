package org.jetlinks.community.device.web.request;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.EmptyByteBuf;
import io.vertx.core.net.impl.PartialPooledByteBufAllocator;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.codec.*;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.supports.official.JetLinksMqttDeviceMessageCodec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;


class ProtocolEncodePayloadTest {

    public static final String DEVICE_ID = "test001";
    public static final String PRODUCT_ID = "test100";

    @Test
    void toDeviceMessage() {
        ProtocolEncodePayload payload = new ProtocolEncodePayload();
        EventMessage eventMessage = new EventMessage();
        eventMessage.setDeviceId("test");
        String s = JSON.toJSONString(eventMessage);
        payload.setPayload("{'event':'{\"id\":\"fire_alarm\",\"name\":\"火警报警\",\"expands\":{\"level\":\"urgent\"},\"valueType\":{\"type\":\"object\",\"properties\":[{\"id\":\"lat\",\"name\":\"纬度\",\"valueType\":{\"type\":\"float\"}},{\"id\":\"point\",\"name\":\"点位\",\"valueType\":{\"type\":\"int\"}},{\"id\":\"lnt\",\"name\":\"经度\",\"valueType\":{\"type\":\"float\"}}]}}],\"properties\":[{\"id\":\"temperature\",\"name\":\"温度\",\"valueType\":{\"type\":\"float\",\"scale\":2,\"unit\":\"celsiusDegrees\"},\"expands\":{\"readOnly\":\"true\",\"source\":\"device\"}}'}");
        payload.toDeviceMessage();

        payload.setTransport(DefaultTransport.MQTT);
        CompositeProtocolSupport support = new CompositeProtocolSupport();


        Subscriber<? super Object> subscriber = new Subscriber<Object>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(Object encodedMessage) {
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        };

        TestDeviceMessageCodec codec1 = new TestDeviceMessageCodec();
        Supplier<Mono<DeviceMessageCodec>> supplier1 = new Supplier<Mono<DeviceMessageCodec>>() {
            @Override
            public Mono<DeviceMessageCodec> get() {
                return Mono.just(codec1);
            }
        };

        support.addMessageCodecSupport(DefaultTransport.MQTT, supplier1);
        payload.doEncode(support, null)
            .subscribe(subscriber);

        JetLinksMqttDeviceMessageCodec codec = Mockito.mock(JetLinksMqttDeviceMessageCodec.class);
        Supplier<Mono<DeviceMessageCodec>> supplier = new Supplier<Mono<DeviceMessageCodec>>() {
            @Override
            public Mono<DeviceMessageCodec> get() {
                return Mono.just(codec);
            }
        };
        support.addMessageCodecSupport(DefaultTransport.MQTT, supplier);

        SimpleMqttMessage mqttMessage = new SimpleMqttMessage();
        mqttMessage.setPayload(new EmptyByteBuf(PartialPooledByteBufAllocator.INSTANCE));
        Mockito.when(codec.encode(Mockito.any(MessageEncodeContext.class)))
            .thenReturn(Mono.just(mqttMessage));


        payload.doEncode(support, null)
            .subscribe(subscriber);
        subscriber.onComplete();


        InterceptorDeviceMessageCodec messageCodec = Mockito.mock(InterceptorDeviceMessageCodec.class);
        Supplier<Mono<DeviceMessageCodec>> supplier2 = new Supplier<Mono<DeviceMessageCodec>>() {
            @Override
            public Mono<DeviceMessageCodec> get() {
                return Mono.just(messageCodec);
            }
        };

        Flux flux = Flux.just(EmptyMessage.INSTANCE);
        Mockito.when(messageCodec.encode(Mockito.any(MessageEncodeContext.class)))
            .thenReturn(flux);
        support.addMessageCodecSupport(DefaultTransport.MQTT, supplier2);
        payload.doEncode(support, null)
            .subscribe(subscriber);
    }
}