package org.jetlinks.community.topic;

import lombok.Generated;
import org.jetlinks.core.utils.StringBuilderUtils;

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

    static void creator(String creatorId, String topic, StringBuilder builder) {
        builder.append("/user/").append(creatorId);
        if (topic.charAt(0) != '/') {
            builder.append('/');
        }
        builder.append(topic);
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
