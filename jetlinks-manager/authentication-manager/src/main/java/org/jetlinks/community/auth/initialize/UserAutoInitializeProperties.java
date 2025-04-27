package org.jetlinks.community.auth.initialize;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.system.authorization.api.entity.UserEntity;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.user-init")
@Component
public class UserAutoInitializeProperties {

    private boolean enabled;

    private List<UserEntity> users;
}
