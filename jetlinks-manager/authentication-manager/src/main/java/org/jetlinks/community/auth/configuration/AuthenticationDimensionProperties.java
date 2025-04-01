package org.jetlinks.community.auth.configuration;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jetlinks.authentication.dimensions")
public class AuthenticationDimensionProperties {


    private Position position = new Position();


    @Getter
    @Setter
    public static class Position {
        /**
         * 是否自动关联组织
         * <p>
         * 无需在用户信息中保存组织信息,通过职位信息自动关联组织
         */
        private boolean autoBindOrg = true;

        /**
         * 是否自动关联角色
         * <p>
         * 无需在用户信息中保存角色信息,通过职位信息自动关联角色
         */
        private boolean autoBindRole = true;
    }
}
