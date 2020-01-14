package org.jetlinks.community.device.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeviceDeployResult {

    private int total;

    private boolean success;

    private String message;

    public static DeviceDeployResult success(int total) {
        return new DeviceDeployResult(total, true, null);
    }

    public static DeviceDeployResult error(String message) {
        return new DeviceDeployResult(0, false, message);
    }
}
