/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
