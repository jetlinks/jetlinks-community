package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;

import java.util.List;

@Getter
@Setter
public class DeviceCategory extends GenericTreeSortSupportEntity<String> {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "标识")
    private String key;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "父节点标识")
    private String parentId;

    @Schema(description = "子节点")
    private List<DeviceCategory> children;


}
