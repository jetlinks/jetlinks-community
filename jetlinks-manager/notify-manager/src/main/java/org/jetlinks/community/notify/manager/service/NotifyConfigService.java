package org.jetlinks.community.notify.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.springframework.stereotype.Service;

@Service
public class NotifyConfigService extends GenericReactiveCacheSupportCrudService<NotifyConfigEntity, String> {

    @Override
    public String getCacheName() {
        return "notify_config";
    }
}
