package org.jetlinks.community.network.mqtt.server;

import org.jetlinks.core.message.codec.MqttMessage;
import org.jetlinks.core.server.mqtt.MqttPublishingMessage;

public interface MqttPublishing extends MqttPublishingMessage {

    MqttMessage getMessage();

    void acknowledge();
}
