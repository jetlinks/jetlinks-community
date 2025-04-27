package org.jetlinks.community.auth.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证信息相关配置项
 * <pre>{@code
 * authentication:
 *   defaults:
 *     user:
 *       userName-demo:
 *         - "*:*"
 *     role:
 *       roleId-demo:
 *         - "device-instance:query,save"
 *     org:
 *       orgId-demo:
 *         - "device-instance:query,save"
 * }
 * </pre>
 *
 * @author gyl
 * @since 2.2
 */
@Slf4j
@ConfigurationProperties(prefix = "jetlinks.authentication")
@Getter
@Setter
public class AuthorizationProperties {

    /**
     * 指定维度类型及维度id配置默认权限
     * <p>在认证信息初始化时，填充合并所在维度的默认权限
     *
     * @see AuthorizationPermissionInitializeService
     */
    private Map</*dimensionType*/String, Map<String, /*permissionText*/List<String>>> defaults = new HashMap<>();

}