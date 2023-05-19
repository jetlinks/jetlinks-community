package org.jetlinks.community.config.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.config.ConfigManager;
import org.jetlinks.community.config.ConfigPropertyDef;
import org.jetlinks.community.config.ConfigScope;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/system/config")
@Resource(id = "system_config", name = "系统配置管理")
@AllArgsConstructor
@Tag(name = "系统配置管理")
public class SystemConfigManagerController {

    private final ConfigManager configManager;

    @GetMapping("/scopes")
    @QueryAction
    @Operation(description = "获取配置作用域")
    public Flux<ConfigScope> getConfigScopes() {
        return configManager.getScopes();
    }

    @GetMapping("/{scope}")
    @Authorize(ignore = true)
    @Operation(description = "获取作用域下的全部配置信息")
    public Mono<Map<String, Object>> getConfigs(@PathVariable String scope) {
        return Authentication
            .currentReactive()
            .hasElement()
            .flatMap(hasAuth -> configManager
                .getScope(scope)
                //公共访问配置或者用户已登录
                .map(conf -> conf.isPublicAccess() || hasAuth)
                //没有定义配置,则用户登录即可访问
                .defaultIfEmpty(hasAuth)
                .filter(Boolean::booleanValue)
                .flatMap(ignore -> configManager.getProperties(scope))
                .map(ValueObject::values))
            .defaultIfEmpty(Collections.emptyMap());
    }

    @GetMapping("/{scope}/_detail")
    @QueryAction
    @Operation(description = "获取作用域下的配置信息")
    public Flux<ConfigPropertyValue> getConfigDetail(@PathVariable String scope) {
        return configManager
            .getProperties(scope)
            .flatMapMany(values -> configManager
                .getPropertyDef(scope)
                .map(def -> ConfigPropertyValue.of(def, values.get(def.getKey()).orElse(null))));

    }

    @PostMapping("/scopes")
    @QueryAction
    @Operation(description = "获取作用域下的配置详情")
    public Flux<Scope> getConfigDetail(@RequestBody Mono<List<String>> scopeMono) {
        return scopeMono
            .flatMapMany(scopes -> Flux
                .fromIterable(scopes)
                .flatMap(scope -> getConfigs(scope)
                    .map(properties -> new Scope(scope, properties))));
    }

    @PostMapping("/{scope}")
    @SaveAction
    @Operation(description = "保存配置")
    public Mono<Void> saveConfig(@PathVariable String scope,
                                 @RequestBody Mono<Map<String, Object>> properties) {
        return properties.flatMap(props -> configManager.setProperties(scope, props));

    }

    @PostMapping("/scope/_save")
    @SaveAction
    @Operation(description = "批量保存配置")
    @Transactional
    public Mono<Void> saveConfig(@RequestBody Flux<Scope> scope) {

        return scope
            .flatMap(scopeConfig -> configManager.setProperties(scopeConfig.getScope(), scopeConfig.getProperties()))
            .then();
    }

    @Getter
    @Setter
    public static class ConfigPropertyValue extends ConfigPropertyDef {
        private Object value;

        public static ConfigPropertyValue of(ConfigPropertyDef def, Object value) {
            ConfigPropertyValue val = FastBeanCopier.copy(def, new ConfigPropertyValue());
            val.setValue(value);
            return val;
        }
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Scope {

        private String scope;

        private Map<String, Object> properties;
    }

}
