package org.jetlinks.community.device.entity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.device.enums.DeviceLogType;
import org.jetlinks.community.things.data.ThingMessageLog;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Generated
public class DeviceOperationLogEntity {
    private static final long serialVersionUID = -6849794470754667710L;

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

    @Schema(description = "消息ID")
    private String messageId;

    @Hidden
    private String orgId;

    @Schema(description = "数据时间")
    private long timestamp;

    public static DeviceOperationLogEntity of(ThingMessageLog log) {
        DeviceOperationLogEntity messageLog = FastBeanCopier.copy(log, new DeviceOperationLogEntity());
        messageLog.setDeviceId(log.getThingId());
        return messageLog;
    }

    public Map<String, Object> toSimpleMap() {
        Map<String, Object> result = FastBeanCopier.copy(this, HashMap::new);
        result.put("type", type.getValue());
        if (content instanceof String) {
            result.put("content", content);
        } else {
            result.put("content", JSON.toJSONString(getContent()));
        }
        return result;
    }

    public DeviceOperationLogEntity generateId() {
        if (StringUtils.isEmpty(id)) {
            setId(DigestUtils.md5Hex(String.join("", deviceId, type.getValue(), String.valueOf(timestamp))));
        }
        return this;
    }
}
