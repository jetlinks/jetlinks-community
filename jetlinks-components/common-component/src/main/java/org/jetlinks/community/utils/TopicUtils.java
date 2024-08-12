package org.jetlinks.community.utils;

import org.jetlinks.core.message.*;
import org.jetlinks.core.message.collector.ReportCollectorDataMessage;
import org.jetlinks.core.message.event.ThingEventMessage;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.springframework.util.StringUtils;
import reactor.function.Consumer3;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class TopicUtils {

    public static final List<Consumer3<Map<String, Object>, String, Set<String>>> TOPIC_REFACTOR_HOOK
        = new CopyOnWriteArrayList<>();

    private static final BiConsumer<Message, StringBuilder>[] TOPIC_BUILDERS;

    static {
        TOPIC_BUILDERS = new BiConsumer[MessageType.values().length];

        //事件
        createFastBuilder(MessageType.EVENT, (message, builder) -> {
            ThingEventMessage event = ((ThingEventMessage) message);
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
        //应答消息 since 1.8
        createFastBuilder(MessageType.ACKNOWLEDGE, "/message/acknowledge");
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
            if (msg instanceof ThingMessage) {
                builder.append("/message/children/")
                       .append(((ThingMessage) msg).getThingId());
            } else {
                builder.append("/message/children");
            }
            appendMessageTopic(msg, builder);
        });
        //子设备消息回复
        createFastBuilder(MessageType.CHILD_REPLY, (message, builder) -> {
            Message msg = ((ChildDeviceMessageReply) message).getChildDeviceMessage();
            if (msg instanceof ThingMessage) {
                builder.append("/message/children/reply/")
                       .append(((ThingMessage) msg).getThingId());
            } else {
                builder.append("/message/children/reply");
            }
            appendMessageTopic(msg, builder);
        });
        //上报了新的物模型
        createFastBuilder(MessageType.DERIVED_METADATA, "/metadata/derived");
        //状态检查
        createFastBuilder(MessageType.STATE_CHECK, "/message/state_check");
        createFastBuilder(MessageType.STATE_CHECK_REPLY, "/message/state_check_reply");

        //数采相关消息 since 2.1
        createFastBuilder(MessageType.REPORT_COLLECTOR, ((message, stringBuilder) -> {
            String addr = message.getHeaderOrElse(ReportCollectorDataMessage.ADDRESS, null);
            stringBuilder.append("/message/collector/report");
            if (StringUtils.hasText(addr)) {
                if (!addr.startsWith("/")) {
                    stringBuilder.append('/');
                }
                stringBuilder.append(addr);
            }
        }));
        createFastBuilder(MessageType.READ_COLLECTOR_DATA, "/message/collector/read");
        createFastBuilder(MessageType.READ_COLLECTOR_DATA_REPLY, "/message/collector/read/reply");
        createFastBuilder(MessageType.WRITE_COLLECTOR_DATA, "/message/collector/write");
        createFastBuilder(MessageType.WRITE_COLLECTOR_DATA_REPLY, "/message/collector/write/reply");

    }

    private static void createFastBuilder(MessageType messageType,
                                          String topic) {
        TOPIC_BUILDERS[messageType.ordinal()] = (ignore, builder) -> builder.append(topic);
    }

    private static void createFastBuilder(MessageType messageType,
                                          BiConsumer<Message, StringBuilder> builderBiConsumer) {
        TOPIC_BUILDERS[messageType.ordinal()] = builderBiConsumer;
    }

    public static String createMessageTopic(Message message, String prefix) {
        return StringBuilderUtils
            .buildString(message, (msg, builder) -> {
                builder.append(prefix);
                TopicUtils.appendMessageTopic(msg, builder);
            });
    }

    public static void appendMessageTopic(Message message, StringBuilder builder) {
        //根据消息类型枚举快速获取拼接器器
        BiConsumer<Message, StringBuilder> fastBuilder = TOPIC_BUILDERS[message.getMessageType().ordinal()];
        if (null != fastBuilder) {
            //执行拼接
            fastBuilder.accept(message, builder);
        } else {
            //不支持的类型,则直接拼接类型
            builder.append("/message/").append(message.getMessageType().name().toLowerCase());
        }
    }

}
