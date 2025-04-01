package org.jetlinks.community.auth.entity;


import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.utils.DigestUtils;

import javax.persistence.Column;
import javax.persistence.Table;

@Getter
@Setter
@Table(name = "s_org_position_role")
@EnableEntityEvent
public class PositionRoleEntity extends GenericEntity<String> {
    private static final long serialVersionUID = 1L;

    @Column(length = 64, nullable = false, updatable = false)
    private String positionId;

    @Column(length = 64, nullable = false, updatable = false)
    private String roleId;

    @Override
    public String getId() {
        if (StringUtils.isEmpty(super.getId()) && positionId != null && roleId != null) {
            super.setId(
                generateId(positionId, roleId)
            );
        }
        return super.getId();
    }

    public static String generateId(String positionId, String roleId) {
        return DigestUtils.md5Hex(positionId + "|" + roleId);
    }
}
