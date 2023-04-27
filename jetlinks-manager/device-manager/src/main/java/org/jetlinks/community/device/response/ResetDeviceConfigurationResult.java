package org.jetlinks.community.device.response;

import lombok.*;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;

/**
 * @see ImportDeviceInstanceResult 结构一致方便前端处理
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResetDeviceConfigurationResult {

    private SaveResult result;

    private boolean success;

    private String message;

    @Generated
    public static ResetDeviceConfigurationResult success(SaveResult result) {
        return new ResetDeviceConfigurationResult(result, true, null);
    }

    @Generated
    public static ResetDeviceConfigurationResult error(String message) {
        return new ResetDeviceConfigurationResult(null, false, message);
    }

    @Generated
    public static ResetDeviceConfigurationResult error(Throwable message) {
        return error(message.getMessage());
    }
}
