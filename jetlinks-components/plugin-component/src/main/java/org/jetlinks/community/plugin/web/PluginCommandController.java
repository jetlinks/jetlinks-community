package org.jetlinks.community.plugin.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.exception.UnAuthorizedException;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.core.command.CommandConstant;
import org.jetlinks.core.utils.CompositeMap;
import org.jetlinks.plugin.core.Plugin;
import org.jetlinks.plugin.core.PluginRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/plugin/command")
@Tag(name = "插件命令执行接口")
public class PluginCommandController {

    private final PluginRegistry registry;

    public PluginCommandController(PluginRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/{type}/{pluginId}/{commandId}")
    @Operation(summary = "GET方式执行插件命令")
    public Mono<ResponseEntity<Object>> callCommand(@PathVariable String type,
                                                    @PathVariable String pluginId,
                                                    @PathVariable String commandId,
                                                    @RequestParam Map<String, Object> params) {
        return this
            .call0(type, pluginId, commandId, params)
            .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/{type}/{pluginId}/{commandId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "POST方式执行插件命令(Json)")
    public Mono<ResponseEntity<Object>> callCommandPost(@PathVariable String type,
                                                        @PathVariable String pluginId,
                                                        @PathVariable String commandId,
                                                        @RequestParam(required = false) Map<String, Object> pathParam,
                                                        @RequestBody Mono<Map<String, Object>> paramsMono) {
        return paramsMono
            .flatMap(params ->
                         call0(type,
                               pluginId,
                               commandId,
                               pathParam == null ? params : new CompositeMap<>(params, pathParam)))
            .map(ResponseEntity::ok);
    }

    @PostMapping(value = "/{type}/{pluginId}/{commandId}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Operation(summary = "POST方式执行插件命令(Form)")
    @SuppressWarnings("all")
    public Mono<ResponseEntity<Object>> callCommandPost(@PathVariable String type,
                                                        @PathVariable String pluginId,
                                                        @PathVariable String commandId,
                                                        ServerWebExchange exchange) {
        Map<String, String> queryParam =  exchange.getRequest().getQueryParams().toSingleValueMap();

        return exchange
            .getFormData()
            .flatMap(params -> call0(type, pluginId, commandId,new CompositeMap<>(queryParam, (Map) params.toSingleValueMap())))
            .map(ResponseEntity::ok);
    }


    private Mono<Object> call0(String type, String pluginId, String commandId, Map<String, Object> param) {
        return registry
            .getPlugin(type, pluginId)
            .switchIfEmpty(Mono.error(NotFoundException.NoStackTrace::new))
            .flatMap(plugin -> call0(plugin, commandId, param));
    }

    private Mono<Object> call0(Plugin plugin, String commandId, Map<String, Object> param) {
        return plugin
            .getCommandMetadata(commandId, param)
            .switchIfEmpty(Mono.error(NotFoundException.NoStackTrace::new))
            .flatMap(meta -> {
                // 允许匿名访问
                if (CommandConstant.isAnonymous(meta)) {
                    return plugin.executeToMono(commandId, param);
                }
                // todo 权限控制
                return Authentication
                    .currentReactive()
                    .switchIfEmpty(Mono.error(UnAuthorizedException.NoStackTrace::new))
                    .then(Mono.defer(() -> plugin.executeToMono(commandId, param)));
            });
    }
}
