package org.jetlinks.community.auth.service;

import lombok.AllArgsConstructor;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.system.authorization.api.event.UserDeletedEvent;
import org.jetlinks.community.auth.entity.ThirdPartyUserBindEntity;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ThirdPartyUserBindService extends GenericReactiveCrudService<ThirdPartyUserBindEntity, String> {

    //订阅用户删除事件，删除第三方用户绑定关系
    @EventListener
    public void handleUserDelete(UserDeletedEvent event) {
        event.async(
            createDelete()
                .where(ThirdPartyUserBindEntity::getUserId, event.getUser().getId())
                .execute()
        );
    }

}
