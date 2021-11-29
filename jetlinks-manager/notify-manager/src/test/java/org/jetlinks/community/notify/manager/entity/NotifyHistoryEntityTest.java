package org.jetlinks.community.notify.manager.entity;

import org.jetlinks.community.notify.email.embedded.EmailTemplate;
import org.jetlinks.community.notify.event.SerializableNotifierEvent;
import org.jetlinks.community.notify.manager.enums.NotifyState;
import org.jetlinks.community.notify.template.Template;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class NotifyHistoryEntityTest {

    @Test
    void of() {
        SerializableNotifierEvent event = new SerializableNotifierEvent();
        event.setContext(new HashMap<>());
        event.setProvider("test");
        event.setNotifyType("test");
        event.setNotifierId("test");
        event.setCause("test");
        event.setSuccess(true);
        event.setErrorType("error");
        event.setTemplate(new EmailTemplate());
        event.setSuccess(false);

        NotifyHistoryEntity entity = NotifyHistoryEntity.of(event);
        entity.setNotifyTime(new Date());
        entity.setRetryTimes(10);
        entity.setState(NotifyState.error);
        entity.setTemplateId("test");
        assertNotNull(entity.getContext());
        assertNotNull(entity.getErrorStack());
        assertNotNull(entity.getErrorType());
        assertNotNull(entity.getErrorStack());
        assertNotNull(entity.getNotifierId());
        assertNotNull(entity.getNotifyTime());
        assertNotNull(entity.getNotifyType());
        assertNotNull(entity.getProvider());
        assertNotNull(entity.getRetryTimes());
        assertNotNull(entity.getState());
        assertNotNull(entity.getTemplate());
        assertNotNull(entity.getTemplateId());

        NotifySubscriberEntity subscriberEntity = new NotifySubscriberEntity();
        subscriberEntity.setSubscribeName("test");
        subscriberEntity.setDescription("test");
        assertNotNull(subscriberEntity.getSubscribeName());
        assertNotNull(subscriberEntity.getDescription());

        NotifyTemplateEntity notifyTemplateEntity = new NotifyTemplateEntity();
        notifyTemplateEntity.setTemplate("test");
        notifyTemplateEntity.setName("test");
        notifyTemplateEntity.setProvider("test");
        assertNotNull(notifyTemplateEntity.getTemplate());
        assertNotNull(notifyTemplateEntity.getName());
        assertNotNull(notifyTemplateEntity.getProvider());
    }
}