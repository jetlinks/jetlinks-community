package org.jetlinks.community.device.web.response;

import lombok.*;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImportDeviceInstanceResult {

    private SaveResult result;

    private boolean success;

    private String message;

    private String detailFile;

    @Generated
    public static ImportDeviceInstanceResult success(SaveResult result) {
        return new ImportDeviceInstanceResult(result, true, null, null);
    }

    @Generated
    public static ImportDeviceInstanceResult error(String message) {
        return new ImportDeviceInstanceResult(null, false, message, null);
    }

    @Generated
    public static ImportDeviceInstanceResult error(Throwable message) {
        return error(message.getMessage());
    }
}
