package org.jetlinks.community.notify.manager.service;

import lombok.Generated;
import org.hswebframework.web.crud.service.GenericReactiveCacheSupportCrudService;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.springframework.stereotype.Service;

@Service
public class NotifyConfigService extends GenericReactiveCacheSupportCrudService<NotifyConfigEntity, String> {

    @Override
    @Generated
    public String getCacheName() {
        return "notify_config";
    }
}
