package org.jetlinks.community.auth.event;

import lombok.AllArgsConstructor;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.api.event.UserDeletedEvent;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DeleteUserListener {
    private final DefaultAuthorizationSettingService authorizationSettingService;

    @EventListener
    public void handleUserDeleteEntity(UserDeletedEvent event) {
        event.async(
            authorizationSettingService
                .createQuery()
                .where(AuthorizationSettingEntity::getDimensionTarget, event.getUser().getId())
                .fetch()
                .map(AuthorizationSettingEntity::getId)
                .collectList()
                .flatMap(list -> authorizationSettingService
                    .createDelete()
                    .where()
                    .in(AuthorizationSettingEntity::getId, list)
                    .execute()
                )
        );
    }
}