package org.jetlinks.community.network.manager.service;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.security.Certificate;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.security.DefaultCertificate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Service
public class CertificateService
        extends GenericReactiveCrudService<CertificateEntity, String>
        implements CertificateManager {
    @Override
    public Mono<Certificate> getCertificate(String id) {
        return createQuery()
                .where(CertificateEntity::getId, id)
                .fetchOne()
                .map(entity -> {
                    DefaultCertificate defaultCertificate = new DefaultCertificate(entity.getId(), entity.getName());
                    return entity.getFormat().init(defaultCertificate, entity.getConfigs());
                });
    }
}
