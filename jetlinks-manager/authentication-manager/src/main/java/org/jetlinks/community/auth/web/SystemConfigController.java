package org.jetlinks.community.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.auth.entity.SystemConfigEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@RequestMapping("/system/config")
@RestController
@Resource(id = "system-config", name = "系统配置")
@Authorize
@Tag(name = "系统配置")
public class SystemConfigController {

    private final ReactiveRepository<SystemConfigEntity, String> repository;

    public SystemConfigController(ReactiveRepository<SystemConfigEntity, String> repository) {
        this.repository = repository;
    }

    @GetMapping("/front")
    @QueryAction
    @Authorize(ignore = true)
    @Operation(summary = "获取前端配置信息")
    public Mono<Map<String, Object>> getFrontConfig() {
        return repository.findById("default")
            .map(SystemConfigEntity::getFrontConfig)
            .defaultIfEmpty(Collections.emptyMap());
    }

    @PostMapping("/front")
    @SaveAction
    @Operation(summary = "保存前端配置信息", description = "参数为json对象,可保存任意字段.")
    public Mono<Void> saveFrontConfig(@RequestBody Mono<Map<String, Object>> config) {
        return config
            .map(front -> SystemConfigEntity.front("default", front))
            .as(repository::save)
            .then();
    }

}
