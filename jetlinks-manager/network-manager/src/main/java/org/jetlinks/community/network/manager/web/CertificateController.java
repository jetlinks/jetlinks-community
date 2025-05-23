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
package org.jetlinks.community.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.network.manager.entity.CertificateEntity;
import org.jetlinks.community.network.manager.service.CertificateService;
import org.jetlinks.community.network.security.Certificate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author wangzheng
 * @since 1.0
 */
@RestController
@RequestMapping("/network/certificate")
@Authorize
@Resource(id = "certificate", name = "证书管理")
@Tag(name = "证书管理")
public class CertificateController implements ReactiveServiceCrudController<CertificateEntity, String> {

    @Autowired
    private CertificateService certificateService;

    @Override
    public CertificateService getService() {
        return certificateService;
    }

    @GetMapping("/{id}/detail")
    @QueryAction
    @Operation(summary = "查看证书信息")
    public Mono<String> getCertificateInfo(@PathVariable String id) {
        return certificateService
            .getCertificate(id)
            .map(Certificate::toString);
    }

    @SaveAction
    @PostMapping("/upload")
    @SneakyThrows
    @Operation(summary = "上传证书并返回证书BASE64")
    public Mono<String> upload(@RequestPart("file")
                               @Parameter(name = "file", description = "文件") Part part) {

        if (part instanceof FilePart) {
            return DataBufferUtils
                .join(part.content())
                .flatMap(buffer -> Mono
                    .fromCallable(() -> Base64.encodeBase64String(StreamUtils.copyToByteArray(buffer.asInputStream(true)))))
                ;
        } else {
            return Mono.error(() -> new IllegalArgumentException("error.not_file"));
        }

    }
}
