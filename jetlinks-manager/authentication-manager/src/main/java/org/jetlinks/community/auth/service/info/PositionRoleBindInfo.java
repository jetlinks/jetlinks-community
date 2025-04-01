package org.jetlinks.community.auth.service.info;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Dimension;
import org.jetlinks.community.auth.entity.RoleEntity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class PositionRoleBindInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String positionId;

    private RoleEntity role;

    public static boolean isPositionBoundRole(Dimension dimension) {
        return dimension.getOption("positionId").isPresent();
    }
}
