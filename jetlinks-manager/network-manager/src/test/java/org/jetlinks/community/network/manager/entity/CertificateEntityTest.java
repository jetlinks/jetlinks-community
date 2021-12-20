package org.jetlinks.community.network.manager.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CertificateEntityTest {
   @Test
    void test(){
       CertificateEntity entity = new CertificateEntity();
       entity.setDescription("test");
       assertNotNull(entity.getDescription());

       CertificateEntity.CertificateConfig config = new CertificateEntity.CertificateConfig();
       config.setTrustKeyStoreBase64("sss");
       config.setTrustKeyStorePwd("sss");
       assertNotNull(config.getTrustKeyStorePwd());
       assertNotNull(config.getTrustKeyStoreBase64());
   }
}