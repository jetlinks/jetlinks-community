package org.jetlinks.community.notify.manager.service;

import org.jetlinks.community.notify.DefaultNotifyType;
import org.jetlinks.community.notify.NotifierProperties;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class DefaultNotifyConfigManagerTest {

  @Test
  void getNotifyConfig() throws Exception {
      NotifyConfigService mock = Mockito.mock(NotifyConfigService.class);
      NotifyConfigEntity notifyConfigEntity = new NotifyConfigEntity();
      notifyConfigEntity.setId("test");
      notifyConfigEntity.setName("test");
      notifyConfigEntity.setProvider("test");
      Mockito.when(mock.findById(Mockito.anyString()))
          .thenReturn(Mono.just(notifyConfigEntity));
      DefaultNotifyConfigManager defaultNotifyConfigManager = new DefaultNotifyConfigManager();
      Field configService = defaultNotifyConfigManager.getClass().getDeclaredField("configService");
      configService.setAccessible(true);
      configService.set(defaultNotifyConfigManager,mock);

      defaultNotifyConfigManager.getNotifyConfig(DefaultNotifyType.weixin,"test")
          .map(NotifierProperties::getName)
          .as(StepVerifier::create)
          .expectNext("test")
          .verifyComplete();

  }
}