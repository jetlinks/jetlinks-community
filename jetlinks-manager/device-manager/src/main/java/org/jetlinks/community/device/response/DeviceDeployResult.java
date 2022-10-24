package org.jetlinks.community.device.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDeployResult {

    private int total;

    private boolean success;

    private String message;

    //导致错误的源头
    private Object source;

    //导致错误的操作
    private String operation;

    @Generated
    public static DeviceDeployResult success(int total) {
        return new DeviceDeployResult(total, true, null, null, null);
    }

    @Generated
    public static DeviceDeployResult error(String message) {
        return new DeviceDeployResult(0, false, message, null, null);
    }
}
