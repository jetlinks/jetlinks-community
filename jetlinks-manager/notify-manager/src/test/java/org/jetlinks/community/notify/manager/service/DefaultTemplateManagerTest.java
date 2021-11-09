package org.jetlinks.community.notify.manager.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.email.embedded.DefaultEmailNotifierProvider;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.sms.TestSmsProvider;
import org.jetlinks.community.notify.template.TemplateManager;
import org.jetlinks.community.notify.template.TemplateProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class DefaultTemplateManagerTest {

  @Test
  void getProperties() throws Exception {
      NotifyTemplateService repository = Mockito.mock(NotifyTemplateService.class);
      NotifyTemplateEntity notifyTemplateEntity = new NotifyTemplateEntity();
      notifyTemplateEntity.setId("test");
      notifyTemplateEntity.setProvider("test");
      notifyTemplateEntity.setType("test");
      notifyTemplateEntity.setTemplate("test");
      Mockito.when(repository.findById(Mockito.any(Mono.class)))
          .thenReturn(Mono.just(notifyTemplateEntity));

      DefaultTemplateManager defaultTemplateManager = new DefaultTemplateManager();
      Class<? extends DefaultTemplateManager> aClass = defaultTemplateManager.getClass();
      Field templateService = aClass.getDeclaredField("templateService");
      templateService.setAccessible(true);
      templateService.set(defaultTemplateManager,repository);
      defaultTemplateManager.getProperties(DefaultNotifyType.weixin,"test")
          .map(TemplateProperties::getTemplate)
          .as(StepVerifier::create)
          .expectNext("test")
          .verifyComplete();

  }

  @Test
  void postProcessAfterInitialization() {
      DefaultTemplateManager service = new DefaultTemplateManager();
      TemplateManager templateManager = Mockito.mock(TemplateManager.class);
      TestSmsProvider testSmsProvider = new TestSmsProvider(templateManager);

      Object provider = service.postProcessAfterInitialization(testSmsProvider, "testSmsProvider");
      assertTrue(provider instanceof TestSmsProvider);
  }
}