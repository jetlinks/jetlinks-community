package org.jetlinks.community.device.web.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDeployResult {

    private int total;

    private boolean success;

    private String message;

    private String sourceId;

    //导致错误的源头
    private Object source;

    //导致错误的操作
    private String operation;

    @Generated
    public static DeviceDeployResult success(int total) {
        return new DeviceDeployResult(total, true, null,null, null, null);
    }

    @Generated
    public static DeviceDeployResult error(String message) {
        return new DeviceDeployResult(0, false, message, null,null, null);
    }

    public static DeviceDeployResult error(String message, String sourceId) {
        return new DeviceDeployResult(0, false, message, sourceId,null, null);
    }
}
