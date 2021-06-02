package org.jetlinks.community.standalone.authorize;

import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.events.AuthorizationSuccessEvent;
import org.jetlinks.community.auth.service.UserDetailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Lind
 * @since 1.0.0
 */
@Component
public class LoginEvent {
    private final UserDetailService detailService;

    public LoginEvent(UserDetailService detailService) {
        this.detailService = detailService;
    }

    @EventListener
    public void handleLoginSuccess(AuthorizationSuccessEvent event) {
        Map<String, Object> result = event.getResult();
        Authentication authentication = event.getAuthentication();
        List<Dimension> dimensions = authentication.getDimensions();

        result.put("permissions", authentication.getPermissions());
        result.put("roles", dimensions);
        result.put("currentAuthority", authentication.getPermissions().stream().map(Permission::getId).collect(Collectors.toList()));

        event.async(
            detailService
                .findUserDetail(event.getAuthentication().getUser().getId())
                .doOnNext(detail -> result.put("user", detail))
        );
    }
}
