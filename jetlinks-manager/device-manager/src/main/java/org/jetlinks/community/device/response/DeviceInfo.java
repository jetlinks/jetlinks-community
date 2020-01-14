package org.jetlinks.community.device.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.device.entity.DeviceInstanceEntity;
import org.jetlinks.community.device.entity.DeviceProductEntity;
import org.jetlinks.community.device.enums.DeviceState;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceInfo {

    private String id;

    //设备实例名称
    private String name;

    private String projectId;

    private String projectName;

    private String classifiedId;

    private String networkWay;

    private String productId;

    private DeviceState state;

    private String creatorId;

    private String creatorName;

    private Long createTime;

    private Long registryTime;

    private String orgId;
    //说明
    private String describe;

    //产品名称
    private String productName;

    private String deviceType;

    //传输协议
    private String transportProtocol;

    //消息协议
    private String messageProtocol;

    private String deriveMetadata;

    private Map<String, Object> configuration;

    public static DeviceInfo of(DeviceInstanceEntity instance,
                                DeviceProductEntity product) {
        DeviceInfo deviceInfo = FastBeanCopier.copy(instance, new DeviceInfo());
        deviceInfo.setMessageProtocol(product.getMessageProtocol());
        deviceInfo.setTransportProtocol(product.getTransportProtocol());
        deviceInfo.setDeviceType(product.getDeviceType().getText());
        deviceInfo.setClassifiedId(product.getClassifiedId());
        deviceInfo.setProjectId(product.getProjectId());
        deviceInfo.setProjectName(product.getProjectName());
        deviceInfo.setProductName(product.getName());
        deviceInfo.setNetworkWay(product.getNetworkWay());
        if (CollectionUtils.isEmpty(instance.getConfiguration())) {
            deviceInfo.setConfiguration(product.getConfiguration());
        }
        if (StringUtils.isEmpty(instance.getDeriveMetadata())) {
            deviceInfo.setDeriveMetadata(product.getMetadata());
        }
        return deviceInfo;
    }
}
