package org.jetlinks.community.topic;

import lombok.Generated;
import org.jetlinks.core.utils.StringBuilderUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface Topics {


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
