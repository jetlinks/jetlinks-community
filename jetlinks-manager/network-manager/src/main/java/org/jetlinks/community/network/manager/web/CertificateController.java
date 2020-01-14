package org.jetlinks.community.network.manager.web;

import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.manager.service.CertificateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@RestController
@RequestMapping("/network/certificate")
@Authorize
@Resource(id = "certificate", name = "证书管理")
public class CertificateController implements ReactiveServiceCrudController<CertificateEntity, String> {

    @Autowired
    private CertificateService certificateService;

    @Override
    public CertificateService getService() {
        return certificateService;
    }

    @Autowired(required = false)
    DataBufferFactory factory = new DefaultDataBufferFactory();

    @SaveAction
    @PostMapping("/upload")
    @SneakyThrows
    public Mono<String> upload(@RequestPart("file") Part part) {

        if (part instanceof FilePart) {
            return (part)
                    .content()
                    .collectList()
                    .flatMap(all -> Mono.fromCallable(() ->
                            Base64.encodeBase64String(StreamUtils.copyToByteArray(factory.join(all).asInputStream()))))
                    ;
        } else {
            return Mono.error(() -> new IllegalArgumentException("[file] part is not a file"));
        }

    }
}
