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


import static org.junit.jupiter.api.Assertions.*;

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
      certificateConfig.setKeystoreBase64("abc");
      certificateConfig.setKeystorePwd("ab");
      certificateConfig.setTrustKeyStoreBase64("abc");
      certificateConfig.setTrustKeyStorePwd("ab");
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
          .expectError()
          .verify();



  }
}