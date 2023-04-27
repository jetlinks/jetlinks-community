package org.jetlinks.community.web;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.jetlinks.community.resource.Resource;
import org.jetlinks.community.resource.ResourceManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/system/resources")
@Hidden
@AllArgsConstructor
public class SystemResourcesController {

    private final ResourceManager resourceManager;

    @GetMapping("/{type}")
    @SneakyThrows
    public Flux<String> getResources(@PathVariable String type) {
        return Authentication
            .currentReactive()
            .filter(auth -> "admin".equals(auth.getUser().getUsername()))
            .switchIfEmpty(Mono.error(UnAuthorizedException::new))
            .flatMapMany(auth -> resourceManager.getResources(type))
            .map(Resource::asString);
    }


}
