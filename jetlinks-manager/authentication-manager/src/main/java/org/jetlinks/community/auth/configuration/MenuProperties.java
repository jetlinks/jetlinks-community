package org.jetlinks.community.auth.configuration;

import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.jetlinks.community.authorize.OrgDimensionType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "menu")
public class MenuProperties {

    //默认只有角色和职位可以绑定菜单
    private Set<String> dimensions = Sets.newHashSet(
        DefaultDimensionType.role.getId(),
        OrgDimensionType.position.getId()
    );

    private Set<String> allowAllMenusUsers = new HashSet<>(Collections.singletonList("admin"));
    private String allowPermission = "menu";

    public boolean isAllowAllMenu(Authentication auth) {
        return allowAllMenusUsers.contains(auth.getUser().getUsername());
//            || auth.hasPermission(allowPermission);
    }
}
