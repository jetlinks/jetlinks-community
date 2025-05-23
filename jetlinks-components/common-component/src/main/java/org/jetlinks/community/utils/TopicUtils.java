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
package org.jetlinks.community.utils;

import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.apache.commons.collections4.MapUtils;
import org.jetlinks.community.PropertyConstants;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.message.*;
import org.jetlinks.core.message.collector.ReportCollectorDataMessage;
import org.jetlinks.core.message.event.ThingEventMessage;
import org.jetlinks.core.message.module.ThingModuleMessage;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.springframework.util.StringUtils;
import reactor.function.Consumer3;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TopicUtils {

    public static final List<Consumer3<Map<String, Object>, String, Consumer<String>>> TOPIC_REFACTOR_HOOK
        = new CopyOnWriteArrayList<>();

    public static Set<String> refactorTopic(Map<String, Object> configs, String original) {
        if (MapUtils.isEmpty(configs)) {
            return Collections.singleton(original);
        }

        Set<String> topics = Sets.newHashSetWithExpectedSize(2);
        //原始的topic
        topics.add(original);

        //创建人
        String creatorId = PropertyConstants.getFromMapOrElse(PropertyConstants.creatorId, configs, () -> null);
        if (StringUtils.hasText(creatorId)) {
            topics.add(org.jetlinks.community.topic.Topics.creator(creatorId, original));
        }

        // 执行hooks
        if (!TOPIC_REFACTOR_HOOK.isEmpty()) {
            for (Consumer3<Map<String, Object>, String, Consumer<String>> hook : TOPIC_REFACTOR_HOOK) {
                hook.accept(configs, original, topics::add);
            }
        }
        return topics;
    }

    public static Set<SeparatedCharSequence> refactorTopic(DeviceMessage message, SeparatedCharSequence original) {
        return refactorTopic(message.getHeaders(), original);
    }

    public static Set<SeparatedCharSequence> refactorTopic(Map<String, Object> configs, SeparatedCharSequence original) {
        if (MapUtils.isEmpty(configs)) {
            return Collections.singleton(original);
        }

        Set<SeparatedCharSequence> container = Sets.newHashSetWithExpectedSize(2);
        container.add(original);

        //创建人
        String creatorId = PropertyConstants.getFromMapOrElse(PropertyConstants.creatorId, configs, () -> null);
        if (StringUtils.hasText(creatorId)) {
            container.add(
                Topics.creator(creatorId, original)
            );
        }

        // 执行hooks
        if (!TOPIC_REFACTOR_HOOK.isEmpty()) {
            for (Consumer3<Map<String, Object>, String, Consumer<String>> hook : TOPIC_REFACTOR_HOOK) {
                hook.accept(configs, original.toString(), (str) -> container.add(SharedPathString.of(str)));
            }
        }
        return container;
    }


    public static Set<String> refactorTopic(ThingMessage message, String original) {
        return refactorTopic(message.getHeaders(), original);
    }

    private static final TopicBuilder[] TOPIC_BUILDERS;

    static {
        TOPIC_BUILDERS = new TopicBuilder[MessageType.values().length];

        {
            SharedPathString shared = SharedPathString.of("/message/event");
            //事件
            createFastBuilder(MessageType.EVENT,
                              (message, builder) -> {
                                  ThingEventMessage event = ((ThingEventMessage) message);
                                  builder.append("/message/event/").append(event.getEvent());
                              },
                              (message, charSequences) -> {
                                  ThingEventMessage event = ((ThingEventMessage) message);
                                  return charSequences.isEmpty()
                                      ? shared.append(event.getEvent())
                                      : charSequences.append(shared).append(event.getEvent());
                              });

        }
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
        {
            SharedPathString shared = SharedPathString.of("/message/children");
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
            }, (message, builder) -> {
                Message msg = ((ChildDeviceMessage) message).getChildDeviceMessage();
                if (msg instanceof ThingMessage) {
                    String thingId = ((ThingMessage) msg).getThingId();
                    builder = builder.isEmpty() ? shared.append(thingId) : builder.append(shared).append(thingId);
                } else {
                    builder = builder.isEmpty() ? shared : builder.append(shared);
                }
                return createMessageTopic(msg, builder);
            });
        }
        {
            SharedPathString shared = SharedPathString.of("/message/children/reply");
            //子设备消息回复
            createFastBuilder(MessageType.CHILD_REPLY,
                              (message, builder) -> {
                                  Message msg = ((ChildDeviceMessageReply) message).getChildDeviceMessage();
                                  if (msg instanceof ThingMessage) {
                                      builder.append("/message/children/reply/")
                                             .append(((ThingMessage) msg).getThingId());
                                  } else {
                                      builder.append("/message/children/reply");
                                  }
                                  appendMessageTopic(msg, builder);
                              },
                              (message, builder) -> {
                                  Message msg = ((ChildDeviceMessageReply) message).getChildDeviceMessage();
                                  if (msg instanceof ThingMessage) {
                                      String thingId = ((ThingMessage) msg).getThingId();
                                      builder = builder.isEmpty() ? shared.append(thingId) : builder.append(shared).append(thingId);
                                  } else {
                                      builder = builder.isEmpty() ? shared : builder.append(shared);
                                  }
                                  return createMessageTopic(msg, builder);
                              });
        }
        //上报了新的物模型
        createFastBuilder(MessageType.DERIVED_METADATA, "/metadata/derived");
        //状态检查
        createFastBuilder(MessageType.STATE_CHECK, "/message/state_check");
        createFastBuilder(MessageType.STATE_CHECK_REPLY, "/message/state_check_reply");

        {
            SharedPathString shared = SharedPathString.of("/message/collector/report");
            //数采相关消息 since 2.1
            createFastBuilder(
                MessageType.REPORT_COLLECTOR,
                ((message, stringBuilder) -> {
                    String addr = message.getHeaderOrElse(ReportCollectorDataMessage.ADDRESS, null);
                    stringBuilder.append("/message/collector/report");
                    if (StringUtils.hasText(addr)) {
                        if (!addr.startsWith("/")) {
                            stringBuilder.append('/');
                        }
                        stringBuilder.append(addr);
                    }
                }),
                (message, charSequences) -> {
                    String addr = message.getHeaderOrElse(ReportCollectorDataMessage.ADDRESS, null);
                    charSequences = charSequences.append(shared);
                    if (StringUtils.hasText(addr)) {
                        charSequences = charSequences.append(SharedPathString.of(addr));
                    }
                    return charSequences;
                });
        }
        createFastBuilder(MessageType.READ_COLLECTOR_DATA, "/message/collector/read");
        createFastBuilder(MessageType.READ_COLLECTOR_DATA_REPLY, "/message/collector/read/reply");
        createFastBuilder(MessageType.WRITE_COLLECTOR_DATA, "/message/collector/write");
        createFastBuilder(MessageType.WRITE_COLLECTOR_DATA_REPLY, "/message/collector/write/reply");

        //模块消息 since 2.3
        createFastBuilder(MessageType.MODULE,
                          (message, builder) -> {
                              ThingModuleMessage msg = ((ThingModuleMessage) message);

                              builder.append("/module/").append(msg.getModule());

                              appendMessageTopic(msg.getMessage(), builder);
                          },
                          (message, builder) -> {
                              ThingModuleMessage msg = ((ThingModuleMessage) message);
                              if (builder.isEmpty()) {
                                  builder = SharedPathString.of(new String[]{
                                      "", "module", msg.getModule()
                                  });
                              } else {
                                  builder = builder
                                      .append("module")
                                      .append(SharedPathString.of(msg.getModule()));
                              }
                              return createMessageTopic(msg.getMessage(), builder);
                          });
    }

    private static void createFastBuilder(MessageType messageType,
                                          String topic) {
        SharedPathString shared = SharedPathString.of(topic);
        TOPIC_BUILDERS[messageType.ordinal()] = new TopicBuilder() {
            @Override
            public void build(Message message, StringBuilder builder) {
                builder.append(topic);
            }

            @Override
            public SeparatedCharSequence build(Message message, SeparatedCharSequence prefix) {
                return prefix.isEmpty() ? shared : prefix.append(shared);
            }
        };
    }

    private static void createFastBuilder(MessageType messageType,
                                          BiConsumer<Message, StringBuilder> builderBiConsumer,
                                          BiFunction<Message, SeparatedCharSequence, SeparatedCharSequence> builderBiConsumer2) {
        TOPIC_BUILDERS[messageType.ordinal()] = new TopicBuilder() {
            @Override
            public void build(Message message, StringBuilder builder) {
                builderBiConsumer.accept(message, builder);
            }

            @Override
            public SeparatedCharSequence build(Message message, SeparatedCharSequence prefix) {
                return builderBiConsumer2.apply(message, prefix);
            }
        };
    }

    public static String createMessageTopic(Message message, String prefix) {
        return StringBuilderUtils
            .buildString(message, prefix, (msg, _prefix, builder) -> {
                builder.append(_prefix);
                TopicUtils.appendMessageTopic(msg, builder);
            });
    }

    @SneakyThrows
    public static SeparatedCharSequence createMessageTopic(Message message,
                                                           SeparatedCharSequence prefix) {
        //根据消息类型枚举快速获取拼接器器
        TopicBuilder fastBuilder = TOPIC_BUILDERS[message.getMessageType().ordinal()];
        if (null != fastBuilder) {
            //执行拼接
            return fastBuilder.build(message, prefix);
        } else {
            //不支持的类型,则直接拼接类型
            return prefix.append("message", message.getMessageType().name().toLowerCase());
        }
    }

    @SneakyThrows
    public static void appendMessageTopic(Message message, StringBuilder builder) {
        //根据消息类型枚举快速获取拼接器器
        TopicBuilder fastBuilder = TOPIC_BUILDERS[message.getMessageType().ordinal()];
        if (null != fastBuilder) {
            //执行拼接
            fastBuilder.build(message, builder);
        } else {
            //不支持的类型,则直接拼接类型
            builder.append("/message/").append(message.getMessageType().name().toLowerCase());
        }
    }

    private interface TopicBuilder {
        void build(Message message, StringBuilder builder);


        default SeparatedCharSequence build(Message message, SeparatedCharSequence prefix) {
            return prefix.append(
                SharedPathString.of(StringBuilderUtils.buildString(message, this::build))
            );
        }
    }

}
