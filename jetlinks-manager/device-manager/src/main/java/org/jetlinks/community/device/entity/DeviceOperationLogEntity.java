package org.jetlinks.community.device.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetlinks.community.device.enums.DeviceLogType;

import java.util.Date;
import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceOperationLogEntity {

    @Schema(description = "日志ID")
    private String id;

    @Schema(description = "设备ID")
    private String deviceId;

    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "日志类型")
    private DeviceLogType type;

    @Schema(description = "创建时间")
    private long createTime;

    @Schema(description = "日志内容")
    private Object content;

    @Hidden
    private String orgId;

    @Schema(description = "数据时间")
    private long timestamp;

    public Map<String, Object> toSimpleMap() {
        Map<String, Object> result = (Map) JSON.toJSON(this);
        result.put("type", type.getValue());
        if (getContent() instanceof String) {
            result.put("content", getContent());
        } else {
            result.put("content", JSON.toJSONString(getContent()));
        }
        return result;
    }
}
