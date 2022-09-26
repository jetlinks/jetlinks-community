package org.jetlinks.community.auth.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Dimension;

@Getter
@Setter
public class RoleInfo {

    private String id;
    private String name;

    public static RoleInfo of(Dimension dimension) {
        RoleInfo detail = new RoleInfo();
        detail.setId(dimension.getId());
        detail.setName(dimension.getName());
        return detail;
    }
}
