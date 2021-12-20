package org.jetlinks.community.network.manager.service;


import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.manager.enums.CertificateType;
import org.jetlinks.community.network.security.Certificate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


class CertificateServiceTest {

  @Test
  void getCertificate() {

      ReactiveRepository<CertificateEntity, String> repository = Mockito.mock(ReactiveRepository.class);
      ReactiveQuery<CertificateEntity> query = Mockito.mock(ReactiveQuery.class);
      CertificateService service = new CertificateService(){
          @Override
          public ReactiveRepository<CertificateEntity, String> getRepository() {
              return repository;
          }
      };
      CertificateEntity certificateEntity = new CertificateEntity();
      certificateEntity.setId("test");
      certificateEntity.setName("test");
      CertificateEntity.CertificateConfig certificateConfig = new CertificateEntity.CertificateConfig();
      certificateConfig.setKeystoreBase64("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");
      certificateConfig.setKeystorePwd("lvchuan");
      certificateConfig.setTrustKeyStoreBase64("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");
      certificateConfig.setTrustKeyStorePwd("lvchuan");
      certificateEntity.setConfigs(certificateConfig);
      certificateEntity.setInstance(CertificateType.JKS);
      Mockito.when(repository.createQuery())
          .thenReturn(query);
      Mockito.when(query.where(Mockito.any(StaticMethodReferenceColumn.class),Mockito.any(Object.class)))
          .thenReturn(query);
      Mockito.when(query.fetchOne())
          .thenReturn(Mono.just(certificateEntity));


      service.getCertificate("test")
          .map(Certificate::getName)
          .as(StepVerifier::create)
          .expectNext("test")
          .verifyComplete();



  }
}