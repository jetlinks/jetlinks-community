package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.things.ThingMetadataType;

import java.util.Map;

@Getter
@Setter
public class DeviceMetadataMappingDetail {

    @Schema(description = "是否自定义了映射")
    private boolean customMapping;

    @Schema(description = "产品ID")
    private String productId;

    @Schema(description = "设备ID,为空时表示映射对产品下所有设备生效")
    private String deviceId;

    @Schema(description = "物模型名称")
    private String metadataName;

    @Schema(description = "物模型类型,如:property")
    private ThingMetadataType metadataType;

    @Schema(description = "物模型ID,如:属性ID")
    private String metadataId;

    @Schema(description = "原始物模型ID")
    private String originalId;

    @Schema(description = "其他配置")
    private Map<String, Object> others;

    @Schema(description = "说明")
    private String description;

    public static DeviceMetadataMappingDetail ofProduct(String productId) {
        DeviceMetadataMappingDetail detail = new DeviceMetadataMappingDetail();
        detail.setProductId(productId);
        return detail;
    }

    public static DeviceMetadataMappingDetail ofDevice(String productId, String deviceId) {
        DeviceMetadataMappingDetail detail = ofProduct(productId);
        detail.setDeviceId(deviceId);
        return detail;
    }

    public DeviceMetadataMappingDetail with(PropertyMetadata metadata) {
        DeviceMetadataMappingDetail detail = new DeviceMetadataMappingDetail();
        detail.setMetadataId(metadata.getId());
        detail.setOriginalId(metadata.getId());
        detail.setMetadataName(metadata.getName());
        detail.setMetadataType(ThingMetadataType.property);
        return detail;
    }

    public DeviceMetadataMappingDetail with(DeviceMetadataMappingEntity mapping) {
        if (null == mapping) {
            return this;
        }
        this.customMapping = true;
        this.description = mapping.getDescription();
        this.others = mapping.getOthers();
        this.originalId = mapping.getOriginalId();
        this.metadataId = mapping.getMetadataId();
        this.metadataType = mapping.getMetadataType();
        return this;
    }

}
