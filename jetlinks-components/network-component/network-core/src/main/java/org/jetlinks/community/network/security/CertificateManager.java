package org.jetlinks.community.network.security;

import reactor.core.publisher.Mono;

public interface CertificateManager {

    Mono<Certificate> getCertificate(String id);

}
