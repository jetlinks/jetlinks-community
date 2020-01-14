package org.jetlinks.community.device.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImportDeviceInstanceResult {

    private SaveResult result;

    private boolean success;

    private String message;

    public static ImportDeviceInstanceResult success(SaveResult result) {
        return new ImportDeviceInstanceResult(result, true, null);
    }

    public static ImportDeviceInstanceResult error(String message) {
        return new ImportDeviceInstanceResult(null, false, message);
    }

    public static ImportDeviceInstanceResult error(Throwable message) {
        return error(message.getMessage());
    }
}
