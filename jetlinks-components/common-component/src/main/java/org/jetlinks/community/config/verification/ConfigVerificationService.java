package org.jetlinks.community.config.verification;

import io.swagger.v3.oas.annotations.Operation;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.exception.BusinessException;
import org.jetlinks.community.config.entity.ConfigEntity;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

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

    @GetMapping(value = PATH_VERIFICATION_URI)
    @Operation(description = "basePath配置验证接口")
    public Mono<String> basePathValidate() {
        return Mono.just("auth:"+PATH_VERIFICATION_URI);
    }


    @EventListener
    public void handleConfigSavedEvent(EntitySavedEvent<ConfigEntity> event){
        //base-path校验
        event.async(
            Flux.fromIterable(event.getEntity())
                .filter(config -> Objects.equals(config.getScope(), "paths"))
                .flatMap(config-> doBasePathValidate(config.getProperties().get("base-path")))
        );
    }


    public Mono<Void> doBasePathValidate(Object basePath) {
        if (basePath == null) {
            return Mono.empty();
        }

        URI uri = URI.create(CastUtils.castString(CastUtils.castString(basePath).concat(PATH_VERIFICATION_URI)));
        if (Objects.equals(uri.getHost(), "127.0.0.1")){
            return Mono.error(new BusinessException("error.base_path_host_error", 500, "127.0.0.1"));
        }
        if (Objects.equals(uri.getHost(), "localhost")){
            return Mono.error(new BusinessException("error.base_path_host_error", 500, "localhost"));
        }

        return webClient
            .get()
            .uri(uri)
            .exchangeToMono(cr -> {
                if (cr.statusCode().is2xxSuccessful()) {
                    return cr.bodyToMono(String.class)
                             .filter(r-> r.contains("auth:"+PATH_VERIFICATION_URI))
                             .switchIfEmpty(Mono.error(()-> new BusinessException("error.base_path_error")));
                }
                return Mono.defer(() -> Mono.error(new BusinessException("error.base_path_error")));
            })
            .timeout(Duration.ofSeconds(3), Mono.error(TimeoutException::new))
            .onErrorResume(err -> {
                while (err != null) {
                    if (err instanceof TimeoutException) {
                        return Mono.error(() -> new BusinessException("error.base_path_validate_request_timeout"));
                    } else if (err instanceof UnknownHostException) {
                        return Mono.error(() -> new BusinessException("error.base_path_DNS_resolution_failed"));
                    }
                    err = err.getCause();
                }
                return Mono.error(() -> new BusinessException("error.base_path_error"));
            })
            .then();
    }
}
