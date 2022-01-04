package org.jetlinks.community.notify.manager.service;

import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityModifyEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.jetlinks.community.notify.DefaultNotifierManager;
import org.jetlinks.community.notify.NotifierManager;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.supports.event.BrokerEventBus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotifierCacheManagerTest {

  @Test
  void handleTemplateSave() throws Exception {
      NotifierManager notifierManager = Mockito.mock(NotifierManager.class);
      NotifierCacheManager service = new NotifierCacheManager(new DefaultTemplateManager(), notifierManager);

      List<NotifyTemplateEntity> list = new ArrayList<>();
      NotifyTemplateEntity notifyTemplateEntity = new NotifyTemplateEntity();
      notifyTemplateEntity.setName("test");
      notifyTemplateEntity.setProvider("test");
      notifyTemplateEntity.setTemplate("test");
      notifyTemplateEntity.setType("test");
      EntitySavedEvent<NotifyTemplateEntity> event = new EntitySavedEvent(list, NotifyTemplateEntity.class);
      service.handleTemplateSave(event);

      Class<? extends NotifierCacheManager> serviceClass = service.getClass();
      Method reloadTemplate = serviceClass.getDeclaredMethod("reloadTemplate", List.class);
      assertNotNull(reloadTemplate);
      reloadTemplate.setAccessible(true);
      Mono<Void> invoke = (Mono<Void>) reloadTemplate.invoke(service, event.getEntity());
      assertNotNull(invoke);
      invoke.as(StepVerifier::create)
          .expectComplete()
          .verify();

  }

  @Test
  void handleTemplateModify() {
      NotifierManager notifierManager = Mockito.mock(NotifierManager.class);
      NotifierCacheManager service = new NotifierCacheManager(new DefaultTemplateManager(), notifierManager);
      List<NotifyTemplateEntity> list = new ArrayList<>();
      NotifyTemplateEntity notifyTemplateEntity = new NotifyTemplateEntity();
      notifyTemplateEntity.setName("test");
      notifyTemplateEntity.setProvider("test");
      notifyTemplateEntity.setTemplate("test");
      notifyTemplateEntity.setType("test");
      assertNotNull(service);
      EntityModifyEvent<NotifyTemplateEntity> event = new EntityModifyEvent(list,new ArrayList<>(), NotifyTemplateEntity.class);
      service.handleTemplateModify(event);
  }

  @Test
  void handleTemplateDelete() {
      NotifierManager notifierManager = Mockito.mock(NotifierManager.class);
      NotifierCacheManager service = new NotifierCacheManager(new DefaultTemplateManager(), notifierManager);
      List<NotifyTemplateEntity> list = new ArrayList<>();
      NotifyTemplateEntity notifyTemplateEntity = new NotifyTemplateEntity();
      notifyTemplateEntity.setName("test");
      notifyTemplateEntity.setProvider("test");
      notifyTemplateEntity.setTemplate("test");
      notifyTemplateEntity.setType("test");
      assertNotNull(service);
      EntityDeletedEvent<NotifyTemplateEntity> event = new EntityDeletedEvent(list, NotifyTemplateEntity.class);
      service.handleTemplateDelete(event);
  }

  @Test
  void handleConfigSave() {
      DefaultNotifierManager defaultNotifierManager = new DefaultNotifierManager(new DefaultNotifyConfigManager(), new BrokerEventBus());
      NotifierCacheManager service = new NotifierCacheManager(new DefaultTemplateManager(), defaultNotifierManager);
      List<NotifyConfigEntity> list = new ArrayList<>();
      NotifyConfigEntity notifyConfigEntity = new NotifyConfigEntity();
      notifyConfigEntity.setId("test");
      notifyConfigEntity.setName("test");
      assertNotNull(service);
      EntitySavedEvent<NotifyConfigEntity> event = new EntitySavedEvent(list, NotifyConfigEntity.class);
      service.handleConfigSave(event);
  }

  @Test
  void handleConfigModify() {
      DefaultNotifierManager defaultNotifierManager = new DefaultNotifierManager(new DefaultNotifyConfigManager(), new BrokerEventBus());
      NotifierCacheManager service = new NotifierCacheManager(new DefaultTemplateManager(), defaultNotifierManager);
      List<NotifyConfigEntity> list = new ArrayList<>();
      NotifyConfigEntity notifyConfigEntity = new NotifyConfigEntity();
      notifyConfigEntity.setId("test");
      notifyConfigEntity.setName("test");
      assertNotNull(service);
      EntityModifyEvent<NotifyConfigEntity> event = new EntityModifyEvent(list,new ArrayList<>(), NotifyConfigEntity.class);
      service.handleConfigModify(event);
  }

  @Test
  void handleConfigDelete() {
      DefaultNotifierManager defaultNotifierManager = new DefaultNotifierManager(new DefaultNotifyConfigManager(), new BrokerEventBus());
      NotifierCacheManager service = new NotifierCacheManager(new DefaultTemplateManager(), defaultNotifierManager);
      List<NotifyConfigEntity> list = new ArrayList<>();
      NotifyConfigEntity notifyConfigEntity = new NotifyConfigEntity();
      notifyConfigEntity.setId("test");
      notifyConfigEntity.setName("test");
      assertNotNull(service);
      EntityDeletedEvent<NotifyConfigEntity> event = new EntityDeletedEvent(list, NotifyConfigEntity.class);
      service.handleConfigDelete(event);
  }
}