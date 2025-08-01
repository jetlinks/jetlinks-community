/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return this
            .findById(id)
            .filter(cert -> cert.getFormat() != null && cert.getConfigs() != null)
            .map(entity -> {
                DefaultCertificate defaultCertificate = new DefaultCertificate(entity.getId(), entity.getName());
                return entity.getFormat().init(defaultCertificate, entity.getConfigs());
            });
    }
}
