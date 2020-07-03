package org.jetlinks.community.standalone.web;

import org.hswebframework.web.authorization.annotation.Authorize;
import org.jetlinks.community.Version;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/system")
@RestController
public class SystemInfoController {

    @GetMapping("/version")
    @Authorize(ignore = true)
    public Mono<Version> getVersion() {
        return Mono.just(Version.current);
    }

}
