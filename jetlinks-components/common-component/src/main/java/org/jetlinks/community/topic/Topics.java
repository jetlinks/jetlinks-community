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
package org.jetlinks.community.topic;

import lombok.Generated;
import org.jetlinks.core.lang.SeparatedCharSequence;
import org.jetlinks.core.lang.SharedPathString;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.community.utils.TopicUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface Topics {

    @Deprecated
    static String org(String orgId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/org/", orgId, topic);
    }

    static String creator(String creatorId, String topic) {
        return StringBuilderUtils.buildString(creatorId, topic, Topics::creator);
    }

    static SeparatedCharSequence creator(String creatorId, SeparatedCharSequence topic) {
        // /user/{creatorId}/{topic}
        return SharedPathString
            .of(new String[]{"", "user", creatorId})
            .append(topic);
    }

    static void creator(String creatorId, String topic, StringBuilder builder) {
        builder.append("/user/").append(creatorId);
        if (topic.charAt(0) != '/') {
            builder.append('/');
        }
        builder.append(topic);
    }

    @Deprecated
    static String tenant(String tenantId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/tenant/", tenantId, topic);
    }

    @Deprecated
    static List<String> tenants(List<String> tenants, String topic) {
        List<String> topics = new ArrayList<>(tenants.size());
        for (String tenant : tenants) {
            topics.add(tenant(tenant, topic));
        }
        return topics;
    }

    @Deprecated
    static String deviceGroup(String groupId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/device-group/", groupId, topic);

    }

    @Deprecated
    static List<String> deviceGroups(List<String> groupIds, String topic) {
        List<String> topics = new ArrayList<>(groupIds.size());
        for (String groupId : groupIds) {
            topics.add(deviceGroup(groupId, topic));
        }
        return topics;
    }

    /**
     * 根据绑定信息构造topic
     *
     * @param bindings 绑定信息
     * @param topic    topic
     * @return topic
     */
    static List<String> bindings(List<Map<String, Object>> bindings, String topic) {
        List<String> topics = new ArrayList<>(bindings.size());
        bindings(bindings, topic, topics::add);
        return topics;
    }

    static void bindings(List<Map<String, Object>> bindings,
                         String topic,
                         Consumer<String> consumer) {
        for (Map<String, Object> binding : bindings) {
            consumer.accept(binding(String.valueOf(binding.get("type")), String.valueOf(binding.get("id")), topic));
        }
    }


    static void bindings(List<Map<String, Object>> bindings,
                         SeparatedCharSequence topic,
                         Consumer<SeparatedCharSequence> consumer) {
        for (Map<String, Object> binding : bindings) {
            consumer.accept(binding(String.valueOf(binding.get("type")), String.valueOf(binding.get("id")), topic));
        }
    }

    static void relations(List<Map<String, Object>> relations,
                          String topic,
                          Consumer<String> consumer) {

        for (Map<String, Object> relation : relations) {
            consumer.accept(
                relation(String.valueOf(relation.get("type")),
                         String.valueOf(relation.get("id")),
                         String.valueOf(relation.get("rel")),
                         topic)
            );
        }

    }

    static void relations(List<Map<String, Object>> relations,
                          SeparatedCharSequence topic,
                          Consumer<SeparatedCharSequence> consumer) {

        for (Map<String, Object> relation : relations) {
            consumer.accept(
                relation(String.valueOf(relation.get("type")),
                         String.valueOf(relation.get("id")),
                         String.valueOf(relation.get("rel")),
                         topic)
            );
        }

    }

    static void binding(String type, String id, String topic, StringBuilder builder) {
        builder.append('/')
               .append(type)
               .append('/')
               .append(id);
        if (topic.charAt(0) != '/') {
            builder.append('/');
        }
        builder.append(topic);
    }

    /**
     * 根据关系构造topic
     * <pre>{@code
     * /rel/{objectType}/{objectId}/{relation}/{topic}
     *
     * 如: /rel/用户/user1/manager/{topic}
     * }</pre>
     *
     * @param objectType 对象类型
     * @param objectId   对象ID
     * @param relation   关系标识
     * @param topic      topic后缀
     * @param builder    StringBuilder
     */
    static void relation(String objectType, String objectId, String relation, String topic, StringBuilder builder) {
        builder.append("/rel/")
               .append(objectType)
               .append('/')
               .append(objectId)
               .append('/')
               .append(relation);
        if (topic.charAt(0) != '/') {
            builder.append('/');
        }
        builder.append(topic);
    }

    static String relation(String objectType, String objectId, String relation, String topic) {
        return StringBuilderUtils.buildString(objectType, objectId, relation, topic, Topics::relation);
    }

    static SeparatedCharSequence relation(String objectType, String objectId, String relation, SeparatedCharSequence topic) {
        return SharedPathString.of(new String[]{"", objectType, objectId, relation}).append(topic);
    }

    static String binding(String type, String id, String topic) {
        return StringBuilderUtils.buildString(type, id, topic, Topics::binding);
    }

    static SeparatedCharSequence binding(String type, String id, SeparatedCharSequence topic) {
        return SharedPathString.of(new String[]{"", type, id}).append(topic);
    }


    @Deprecated
    static String tenantMember(String memberId, String topic) {
        if (!topic.startsWith("/")) {
            topic = "/" + topic;
        }
        return String.join("", "/member/", memberId, topic);
    }

    @Deprecated
    static List<String> tenantMembers(List<String> members, String topic) {
        return members
            .stream()
            .map(id -> tenantMember(id, topic))
            .collect(Collectors.toList());
    }

    String allDeviceRegisterEvent = "/_sys/registry-device/*/register";
    String allDeviceUnRegisterEvent = "/_sys/registry-device/*/unregister";
    String allDeviceMetadataChangedEvent = "/_sys/registry-device/*/metadata";

    @Generated
    static String deviceRegisterEvent(String deviceId) {
        return registryDeviceEvent(deviceId, "register");
    }

    @Generated
    static String deviceUnRegisterEvent(String deviceId) {
        return registryDeviceEvent(deviceId, "unregister");
    }

    @Generated
    static String deviceMetadataChangedEvent(String deviceId) {
        return registryDeviceEvent(deviceId, "metadata");
    }

    String allProductRegisterEvent = "/_sys/registry-product/*/register";
    String allProductUnRegisterEvent = "/_sys/registry-product/*/unregister";
    String allProductMetadataChangedEvent = "/_sys/registry-product/*/metadata";

    @Generated
    static String productRegisterEvent(String deviceId) {
        return registryProductEvent(deviceId, "register");
    }

    @Generated
    static String productUnRegisterEvent(String deviceId) {
        return registryProductEvent(deviceId, "unregister");
    }

    @Generated
    static String productMetadataChangedEvent(String deviceId) {
        return registryProductEvent(deviceId, "metadata");
    }


    static String registryDeviceEvent(String deviceId, String event) {
        return "/_sys/registry-device/" + deviceId + "/" + event;
    }

    static String registryProductEvent(String deviceId, String event) {
        return "/_sys/registry-product/" + deviceId + "/" + event;
    }


    /**
     * 重构topic,将topic拼接上租户等信息的前缀
     *
     * @param topic   topic
     * @param configs 包含的信息
     * @return
     */
    @SuppressWarnings("all")
    static Set<String> refactorTopic(String topic, Map<String, Object> configs) {
        return TopicUtils.refactorTopic(configs, topic);
    }

    static String alarm(String targetType, String targetId, String alarmId) {
        //  /alarm/{targetType}/{targetId}/{alarmId}/record
        return String.join("", "/alarm/", targetType, "/", targetId, "/", alarmId, "/record");
    }

    static String alarmHandleHistory(String targetType, String targetId, String alarmId) {
        //  /alarm/{targetType}/{targetId}/{alarmId}/handle-history
        return String.join("", "/alarm/", targetType, "/", targetId, "/", alarmId, "/handle-history");
    }

    static String alarmLog(String targetType, String targetId, String alarmId, String recordId) {
        //  /alarm/{targetType}/{targetId}/{alarmId}/{recordId}/log
        return String.join("", "/alarm/", targetType, "/", targetId, "/", alarmId, "/", recordId, "/log");
    }

    static String alarmRelieve(String targetType, String targetId, String alarmId) {
        //  /alarm/{targetType}/{targetId}/{alarmId}/relieve
        return String.join("", "/alarm/", targetType, "/", targetId, "/", alarmId, "/relieve");
    }

    interface Authentications {

        String allUserAuthenticationChanged = "/_sys/user-dimension-changed/*";

        /**
         * @see org.hswebframework.web.authorization.Authentication
         */
        static String userAuthenticationChanged(String userId) {
            return "/_sys/user-dimension-changed/" + userId;
        }

    }
}
