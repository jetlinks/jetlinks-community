package org.jetlinks.community.device.message;

import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.event.EventMessage;
import org.jetlinks.core.server.MessageHandler;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 将设备消息连接到消息网关
 *
 * @author zhouhao
 * @since 1.0
 */
@Slf4j
public class DeviceMessageConnector implements DecodedClientMessageHandler {

    //将设备注册中心的配置追加到消息header中,下游订阅者可直接使用.
    private final static String[] allConfigHeader = {
        PropertyConstants.productId.getKey(),
        PropertyConstants.deviceName.getKey(),
        PropertyConstants.orgId.getKey()
    };

    //设备注册中心
    private final DeviceRegistry registry;

    private final EventBus eventBus;

    private final MessageHandler messageHandler;

    private final static BiConsumer<Throwable, Object> doOnError = (error, val) -> DeviceMessageConnector.log.error(error.getMessage(), error);

    private final static Function<DeviceOperator, Mono<Values>> configGetter = operator -> operator.getSelfConfigs(allConfigHeader);

    private final static Values emptyValues = Values.of(Collections.emptyMap());

    public DeviceMessageConnector(EventBus eventBus,
                                  DeviceRegistry registry,
                                  MessageHandler messageHandler,
                                  DeviceSessionManager sessionManager) {
        this.registry = registry;
        this.eventBus = eventBus;
        this.messageHandler = messageHandler;
        sessionManager
            .onRegister()
            .flatMap(session -> {
                DeviceOnlineMessage message = new DeviceOnlineMessage();
                message.setDeviceId(session.getDeviceId());
                message.setTimestamp(session.connectTime());
                return onMessage(message);
            })
            .onErrorContinue(doOnError)
            .subscribe();

        sessionManager
            .onUnRegister()
            .flatMap(session -> {
                DeviceOfflineMessage message = new DeviceOfflineMessage();
                message.setDeviceId(session.getDeviceId());
                message.setTimestamp(System.currentTimeMillis());
                return onMessage(message);
            })
            .onErrorContinue(doOnError)
            .subscribe();
    }

    public Mono<Void> onMessage(Message message) {
        if (null == message) {
            return Mono.empty();
        }
        return this
            .getTopic(message)
            .flatMap(topic -> eventBus.publish(topic, message).then())
            .onErrorContinue(doOnError)
            .then();
    }

    private Flux<String> getTopic(Message message) {
        Flux<String> topicsStream = createDeviceMessageTopic(registry, message);
        if (message instanceof ChildDeviceMessage) { //子设备消息
            return this
                .onMessage(((ChildDeviceMessage) message).getChildDeviceMessage())
                .thenMany(topicsStream);
        } else if (message instanceof ChildDeviceMessageReply) { //子设备消息
            return this
                .onMessage(((ChildDeviceMessageReply) message).getChildDeviceMessage())
                .thenMany(topicsStream);
        }
        return topicsStream;
    }

    public static Flux<String> createDeviceMessageTopic(DeviceRegistry deviceRegistry, Message message) {
        return Flux.defer(() -> {
            if (message instanceof DeviceMessage) {
                DeviceMessage deviceMessage = ((DeviceMessage) message);
                String deviceId = deviceMessage.getDeviceId();
                if (deviceId == null) {
                    log.warn("无法从消息中获取设备ID:{}", deviceMessage);
                    return Mono.empty();
                }
                return deviceRegistry
                    .getDevice(deviceId)
                    .flatMap(configGetter)
                    .defaultIfEmpty(emptyValues)
                    .flatMapIterable(configs -> {
                        configs.getAllValues().forEach(deviceMessage::addHeader);
                        String productId = deviceMessage.getHeader(PropertyConstants.productId).orElse("null");
                        String topic = createDeviceMessageTopic(productId, deviceId, deviceMessage);
                        List<String> topics = new ArrayList<>(2);
                        topics.add(topic);
                        configs.getValue(PropertyConstants.orgId)
                               .ifPresent(orgId -> topics.add("/org/" + orgId + topic));

                        return topics;
                    });
            }
            return Mono.just("/device/unknown/message/unknown");
        });
    }

    public static String createDeviceMessageTopic(String productId, String deviceId, DeviceMessage message) {
        StringBuilder builder = new StringBuilder(64)
            .append("/device/")
            .append(productId)
            .append("/")
            .append(deviceId);

        appendDeviceMessageTopic(message, builder);
        return builder.toString();
    }

    private static final BiConsumer<Message, StringBuilder>[] fastTopicBuilder;

    static {
        fastTopicBuilder = new BiConsumer[MessageType.values().length];


        //事件
        createFastBuilder(MessageType.EVENT, (message, builder) -> {
            EventMessage event = ((EventMessage) message);
            builder.append("/message/event/").append(event.getEvent());
        });

        //上报属性
        createFastBuilder(MessageType.REPORT_PROPERTY, "/message/property/report");
        //读取属性
        createFastBuilder(MessageType.READ_PROPERTY, "/message/send/property/read");
        //读取属性回复
        createFastBuilder(MessageType.READ_PROPERTY_REPLY, "/message/property/read/reply");
        //修改属性
        createFastBuilder(MessageType.WRITE_PROPERTY, "/message/send/property/write");
        //修改属性回复
        createFastBuilder(MessageType.WRITE_PROPERTY_REPLY, "/message/property/write/reply");
        //调用功能
        createFastBuilder(MessageType.INVOKE_FUNCTION, "/message/send/function");
        //调用功能回复
        createFastBuilder(MessageType.INVOKE_FUNCTION_REPLY, "/message/function/reply");
        //注册
        createFastBuilder(MessageType.REGISTER, "/register");
        //注销
        createFastBuilder(MessageType.UN_REGISTER, "/unregister");
        //拉取固件
        createFastBuilder(MessageType.REQUEST_FIRMWARE, "/firmware/pull");
        //拉取固件回复
        createFastBuilder(MessageType.REQUEST_FIRMWARE_REPLY, "/firmware/pull/reply");
        //上报固件信息
        createFastBuilder(MessageType.REPORT_FIRMWARE, "/firmware/report");
        //上报固件安装进度
        createFastBuilder(MessageType.UPGRADE_FIRMWARE_PROGRESS, "/firmware/progress");
        //推送固件
        createFastBuilder(MessageType.UPGRADE_FIRMWARE, "/firmware/push");
        //推送固件回复
        createFastBuilder(MessageType.UPGRADE_FIRMWARE_REPLY, "/firmware/push/reply");
        //未知
        createFastBuilder(MessageType.UNKNOWN, "/message/unknown");
        //日志
        createFastBuilder(MessageType.LOG, "/message/log");
        //透传
        createFastBuilder(MessageType.DIRECT, "/message/direct");
        //更新标签
        createFastBuilder(MessageType.UPDATE_TAG, "/message/tags/update");
        //上线
        createFastBuilder(MessageType.ONLINE, "/online");
        //离线
        createFastBuilder(MessageType.OFFLINE, "/offline");
        //断开连接
        createFastBuilder(MessageType.DISCONNECT, "/disconnect");
        //断开连接回复
        createFastBuilder(MessageType.DISCONNECT_REPLY, "/disconnect/reply");
        //子设备消息
        createFastBuilder(MessageType.CHILD, (message, builder) -> {
            Message msg = ((ChildDeviceMessage) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                builder.append("/message/children/")
                       .append(((DeviceMessage) msg).getDeviceId());
            } else {
                builder.append("/message/children");
            }
            appendDeviceMessageTopic(msg, builder);
        });
        //子设备消息回复
        createFastBuilder(MessageType.CHILD_REPLY, (message, builder) -> {
            Message msg = ((ChildDeviceMessageReply) message).getChildDeviceMessage();
            if (msg instanceof DeviceMessage) {
                builder.append("/message/children/reply/")
                       .append(((DeviceMessage) msg).getDeviceId());
            } else {
                builder.append("/message/children/reply");
            }
            appendDeviceMessageTopic(msg, builder);
        });
        //上报了新的物模型
        createFastBuilder(MessageType.DERIVED_METADATA, "/metadata/derived");
    }

    private static void createFastBuilder(MessageType messageType,
                                          String topic) {
        fastTopicBuilder[messageType.ordinal()] = (ignore, builder) -> builder.append(topic);
    }

    private static void createFastBuilder(MessageType messageType,
                                          BiConsumer<Message, StringBuilder> builderBiConsumer) {
        fastTopicBuilder[messageType.ordinal()] = builderBiConsumer;
    }

    public static void appendDeviceMessageTopic(Message message, StringBuilder builder) {

        BiConsumer<Message, StringBuilder> fastBuilder = fastTopicBuilder[message.getMessageType().ordinal()];
        if (null != fastBuilder) {
            fastBuilder.accept(message, builder);
        } else {
            builder.append("/message/").append(message.getMessageType().name().toLowerCase());
        }
    }

    protected Mono<Boolean> handleChildrenDeviceMessage(Message message) {
        if (message instanceof DeviceMessageReply) {
            return doReply(((DeviceMessageReply) message));
        }
        //不处理子设备上下线,统一由 DeviceGatewayHelper处理
        return Mono.just(true);
    }

    protected Mono<Boolean> handleChildrenDeviceMessageReply(ChildDeviceMessage reply) {
        return handleChildrenDeviceMessage(reply.getChildDeviceMessage());
    }

    protected Mono<Boolean> handleChildrenDeviceMessageReply(ChildDeviceMessageReply reply) {
        return handleChildrenDeviceMessage(reply.getChildDeviceMessage());
    }

    @Override
    public Mono<Boolean> handleMessage(DeviceOperator device, @Nonnull Message message) {
        Mono<Boolean> then;
        if (message instanceof ChildDeviceMessageReply) {
            then = handleChildrenDeviceMessageReply(((ChildDeviceMessageReply) message));
        } else if (message instanceof ChildDeviceMessage) {
            then = handleChildrenDeviceMessageReply(((ChildDeviceMessage) message));
        } else if (message instanceof DeviceMessageReply) {
            then = doReply(((DeviceMessageReply) message));
        } else {
            then = Mono.just(true);
        }
        return this
            .onMessage(message)
            .then(then)
            .defaultIfEmpty(false);

    }

    private Mono<Boolean> doReply(DeviceMessageReply reply) {
        if (log.isDebugEnabled()) {
            log.debug("reply message {}", reply.getMessageId());
        }
        return messageHandler
            .reply(reply)
            .thenReturn(true)
            .doOnError((error) -> log.error("reply message error", error))
            ;
    }
}
