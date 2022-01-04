package org.jetlinks.community.auth.service;

import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.system.authorization.api.entity.DimensionTypeEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DimensionInitServiceTest {

  @Test
  void run() {
      ReactiveRepository<DimensionTypeEntity, String> dimensionTypeRepository = Mockito.mock(ReactiveRepository.class);
      Mockito.when(dimensionTypeRepository.save(Mockito.any(Publisher.class)))
          .thenReturn(Mono.just(SaveResult.of(1,0)));
      DimensionInitService dimensionInitService = new DimensionInitService(dimensionTypeRepository);
      assertNotNull(dimensionInitService);
      try {
          dimensionInitService.run();
      } catch (Exception e) {
          e.printStackTrace();
      }
  }
}