package org.jetlinks.community.device.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.device.entity.DevicePropertiesEntity;
import org.jetlinks.community.device.enums.DeviceState;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Getter
@Setter
public class DeviceAllInfoResponse {

    /**
     * 设备基本信息
     */
    private DeviceInfo deviceInfo;

    /**
     * 设备真实状态
     */
    private DeviceState realState;

    /**
     * 设备上线时间
     */
    private long onlineTime;

    /**
     * 设备离线时间
     */
    private long offlineTime;

    /**
     * 元数据  属性id:属性值 映射
     */
    private Map<String, Object> properties;

    /**
     * 元数据  事件id:事件数量 映射
     */
    private Map<String, Integer> eventCounts;

    public static DeviceAllInfoResponse of(DeviceInfo deviceInfo, DeviceRunInfo deviceRunInfo) {
        DeviceAllInfoResponse info = new DeviceAllInfoResponse();
        info.setDeviceInfo(deviceInfo);
        info.setOfflineTime(deviceRunInfo.getOfflineTime());
        info.setOnlineTime(deviceRunInfo.getOnlineTime());
        info.setRealState(deviceRunInfo.getState());
        return info;
    }


    public DeviceAllInfoResponse ofProperties(List<DevicePropertiesEntity> properties) {
        this.setProperties(properties.stream().collect(Collectors.toMap(DevicePropertiesEntity::getProperty, DevicePropertiesEntity::getFormatValue)));
        return this;
    }

    public DeviceAllInfoResponse ofEventCounts(Map<String, Integer> eventCounts) {
        this.setEventCounts(eventCounts);
        return this;
    }
}
