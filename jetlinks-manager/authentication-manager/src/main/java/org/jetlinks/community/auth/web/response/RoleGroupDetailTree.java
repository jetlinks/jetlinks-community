package org.jetlinks.community.auth.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.auth.entity.RoleEntity;
import org.jetlinks.community.auth.entity.RoleGroupEntity;

import java.util.List;

/**
 * @author gyl
 * @since 2.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class RoleGroupDetailTree {

    private String groupId;
    private String groupName;
    private List<RoleEntity> roles;


    public static RoleGroupDetailTree of(RoleGroupEntity group) {
        RoleGroupDetailTree roleGroupDetailTree = new RoleGroupDetailTree();
        roleGroupDetailTree.setGroupId(group.getId());
        roleGroupDetailTree.setGroupName(group.getName());
        return roleGroupDetailTree;
    }


}
