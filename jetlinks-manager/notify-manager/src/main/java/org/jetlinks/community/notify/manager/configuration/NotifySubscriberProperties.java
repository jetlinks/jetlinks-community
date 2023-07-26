package org.jetlinks.community.notify.manager.configuration;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhangji 2023/6/15
 * @since 2.1
 */
@Getter
@Setter
@ConfigurationProperties("notify.subscriber")
public class NotifySubscriberProperties {
    private Set<String> allowAllNotifyUsers = new HashSet<>(Collections.singletonList("admin"));

    public boolean isAllowAllNotify(Authentication auth) {
        return allowAllNotifyUsers.contains(auth.getUser().getUsername());
    }
}
