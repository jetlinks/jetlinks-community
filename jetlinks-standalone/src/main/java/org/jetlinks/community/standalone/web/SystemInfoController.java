package org.jetlinks.community.standalone.web;

import lombok.AllArgsConstructor;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.community.Version;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequestMapping("/system")
@RestController
@AllArgsConstructor
public class SystemInfoController {

    private final ApiInfoProperties apiInfoProperties;

    @GetMapping("/version")
    @Authorize(ignore = true)
    public Mono<Version> getVersion() {
        return Mono.just(Version.current);
    }

    @GetMapping("/apis")
    @Authorize(ignore = true)
    public Mono<ApiInfoProperties> getApis() {
        return Mono.just(apiInfoProperties);
    }

}
