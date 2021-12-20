package org.jetlinks.community.device.web.request;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.jetlinks.core.defaults.CompositeProtocolSupport;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolDecodePayloadTest {

    @Test
    void toEncodedMessage() {
        ProtocolDecodePayload protocolDecodePayload = new ProtocolDecodePayload();
        protocolDecodePayload.setTransport(DefaultTransport.MQTT);

        SimpleMqttMessage simpleMqttMessage = new SimpleMqttMessage();
        simpleMqttMessage.setClientId("test");
        simpleMqttMessage.setTopic("test");
        simpleMqttMessage.setMessageId(1);
        String s = JSON.toJSONString(simpleMqttMessage, SerializerFeature.IgnoreErrorGetter);
        protocolDecodePayload.setPayload(s);
        EncodedMessage encodedMessage = protocolDecodePayload.toEncodedMessage();
        assertNotNull(encodedMessage);

        protocolDecodePayload.setTransport(DefaultTransport.CoAP);
        EncodedMessage message = EmptyMessage.INSTANCE;
//        DefaultCoapMessage defaultCoapMessage = new DefaultCoapMessage();
        String s1 = JSON.toJSONString(message, SerializerFeature.IgnoreErrorGetter);
        protocolDecodePayload.setPayload(s1);
        Executable executable = ()->protocolDecodePayload.toEncodedMessage();
        assertThrows(NoClassDefFoundError.class,executable);

        protocolDecodePayload.setTransport(DefaultTransport.TCP);
        EncodedMessage encodedMessage2 = protocolDecodePayload.toEncodedMessage();
        assertNotNull(encodedMessage2);

    }

    @Test
    void doDecode() {
        ProtocolDecodePayload protocolDecodePayload = new ProtocolDecodePayload();
        protocolDecodePayload.setTransport(DefaultTransport.MQTT);

        CompositeProtocolSupport support = new CompositeProtocolSupport();


        Supplier<Mono<DeviceMessageCodec>> supplier = new Supplier<Mono<DeviceMessageCodec>>() {
            @Override
            public Mono<DeviceMessageCodec> get() {
                return Mono.just(new TestDeviceMessageCodec());
            }
        };
        support.addMessageCodecSupport(DefaultTransport.MQTT,supplier);

        Subscriber<? super Message> subscriber = new Subscriber<Message>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(1);
            }

            @Override
            public void onNext(Message message) {

            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {

            }
        };
        protocolDecodePayload.doDecode(support,null)
            .subscribe(subscriber);
    }
}