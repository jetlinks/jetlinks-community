package org.jetlinks.community.device.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.api.crud.entity.GenericTreeSortSupportEntity;

import java.util.List;

@Getter
@Setter
public class DeviceCategory extends GenericTreeSortSupportEntity<String> {

    private String id;

    private String key;

    private String name;

    private String parentId;

    private List<DeviceCategory> children;


}
