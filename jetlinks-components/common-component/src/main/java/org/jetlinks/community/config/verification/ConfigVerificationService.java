package org.jetlinks.community.config.verification;

import io.swagger.v3.oas.annotations.Operation;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author bestfeng
 */
@RestController
public class ConfigVerificationService {


    private final WebClient webClient;

    private static final String PATH_VERIFICATION_URI = "/system/config/base-path/verification";

    public ConfigVerificationService() {
        this.webClient = WebClient
            .builder()
            .build();
    }

    @GetMapping(PATH_VERIFICATION_URI)
    @Operation(description = "basePath配置验证接口")
    public Mono<Void> basePathValidate() {
        return Mono.empty();
    }



    public Mono<Void> doBasePathValidate(Object basePath) {
        if (basePath == null){
            return Mono.empty();
        }
        return webClient
            .get()
            .uri(CastUtils.castString(basePath).concat(PATH_VERIFICATION_URI))
            .exchangeToMono(cr -> {
                if (cr.statusCode().is2xxSuccessful()) {
                    return Mono.empty();
                }
                return Mono.defer(() -> Mono.error(new BusinessException("error.base_path_error")));
            })
            .onErrorResume(err-> Mono.defer(() -> Mono.error(new BusinessException("error.base_path_error"))))
            .then();
    }


}
