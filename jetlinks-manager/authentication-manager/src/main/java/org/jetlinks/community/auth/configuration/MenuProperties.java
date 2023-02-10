package org.jetlinks.community.auth.configuration;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "menu")
public class MenuProperties {

    private Set<String> allowAllMenusUsers = new HashSet<>(Collections.singletonList("admin"));

    public boolean isAllowAllMenu(Authentication auth) {
        return allowAllMenusUsers.contains(auth.getUser().getUsername());
//            || auth.hasPermission(allowPermission);
    }
}
