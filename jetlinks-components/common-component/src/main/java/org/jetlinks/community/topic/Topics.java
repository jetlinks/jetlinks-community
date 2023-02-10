package org.jetlinks.community.topic;

import lombok.Generated;
import org.jetlinks.core.utils.StringBuilderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface Topics {

    static String creator(String creatorId, String topic) {
        return StringBuilderUtils.buildString(creatorId, topic, Topics::creator);
    }

    static void creator(String creatorId, String topic, StringBuilder builder) {
        builder.append("/user/").append(creatorId);
        if (topic.charAt(0) != '/') {
            builder.append('/');
        }
        builder.append(topic);
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
        for (Map<String, Object> binding : bindings) {
            topics.add(binding(String.valueOf(binding.get("type")), String.valueOf(binding.get("id")), topic));
        }
        return topics;
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

    static String binding(String type, String id, String topic) {
        return StringBuilderUtils.buildString(type, id, topic, Topics::binding);
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

    static String alarm(String targetType, String targetId, String alarmId) {
        //  /alarm/{targetType}/{targetId}/{alarmId}/record
        return String.join("", "/alarm/", targetType, "/", targetId, "/", alarmId, "/record");
    }
}
