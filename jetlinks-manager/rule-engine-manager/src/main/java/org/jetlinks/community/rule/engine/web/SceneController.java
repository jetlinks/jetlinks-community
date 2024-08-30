package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.authorization.annotation.DeleteAction;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.rule.engine.service.SceneService;
import org.jetlinks.community.rule.engine.web.request.SceneExecuteRequest;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.community.rule.engine.entity.SceneEntity;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProvider;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.scene.*;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


@RestController
@RequestMapping("/scene")
@Tag(name = "场景管理")
@AllArgsConstructor
@Resource(id = "rule-scene", name = "场景管理")
public class SceneController implements ReactiveServiceQueryController<SceneEntity, String> {

    @Getter
    private final SceneService service;

    @PostMapping
    @Operation(summary = "创建场景")
    @SaveAction
    public Mono<SceneEntity> createScene(@RequestBody Mono<SceneRule> sceneRuleMono) {
        return sceneRuleMono
            .flatMap(service::createScene);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新场景")
    @SaveAction
    public Mono<Void> update(@PathVariable String id,
                             @RequestBody Mono<SceneRule> sceneRuleMono) {
        return sceneRuleMono
            .flatMap(sceneRule -> service.updateScene(id, sceneRule))
            .then();
    }

    @PutMapping("/{id}/_disable")
    @Operation(summary = "禁用场景")
    @SaveAction
    public Mono<Void> disableScene(@PathVariable String id) {
        return service.disabled(id);
    }

    @PutMapping("/batch/_disable")
    @Operation(summary = "批量禁用场景")
    @SaveAction
    public Mono<Void> disableSceneBatch(@RequestBody Mono<List<String>> id) {
        return id.flatMap(service::disabled);
    }

    @PutMapping("/{id}/_enable")
    @Operation(summary = "启用场景")
    @SaveAction
    public Mono<Void> enabledScene(@PathVariable String id) {
        return service.enable(id);
    }

    @PutMapping("/batch/_enable")
    @Operation(summary = "批量启用场景")
    @SaveAction
    public Mono<Void> enabledSceneBatch(@RequestBody Mono<List<String>> id) {
        return id.flatMap(service::enable);
    }

    @PostMapping("/{id}/_execute")
    @Operation(summary = "手动执行场景")
    @SaveAction
    public Mono<Void> execute(@PathVariable String id,
                              @RequestBody Mono<Map<String, Object>> context) {
        return context.flatMap(ctx -> service.execute(id, ctx));
    }

    @PostMapping("/batch/_execute")
    @Operation(summary = "批量手动执行场景")
    @SaveAction
    public Mono<Void> executeBatch(@RequestBody Flux<SceneExecuteRequest> request) {
        return service.executeBatch(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除场景")
    @DeleteAction
    public Mono<Void> removeScene(@PathVariable String id) {
        return service
            .deleteById(id)
            .then();
    }

    @PostMapping("/parse-term-column")
    @Operation(summary = "根据触发器解析出支持的条件列")
    @QueryAction
    public Flux<TermColumn> parseTermColumns(@RequestBody Mono<SceneRule> ruleMono) {
        return ruleMono
            .flatMapMany(rule -> {
                Trigger trigger = rule.getTrigger();
                if (trigger != null) {
                    return trigger.parseTermColumns();
                }
                return Flux.empty();
            });
    }

    @PostMapping("/parse-variables")
    @Operation(summary = "解析规则中输出的变量")
    @QueryAction
    public Flux<Variable> parseVariables(@RequestBody Mono<SceneRule> ruleMono,
                                         @RequestParam(required = false) Integer branch,
                                         @RequestParam(required = false) Integer branchGroup,
                                         @RequestParam(required = false) Integer action) {
        Mono<SceneRule> cache = ruleMono.cache();
        return Mono
            .zip(
                parseTermColumns(cache).collectList(),
                cache,
                (columns, rule) -> rule
                    .createVariables(columns,
                                     branch,
                                     branchGroup,
                                     action))
            .flatMapMany(Function.identity());
    }

    @GetMapping("/device-selectors")
    @Operation(summary = "获取支持的设备选择器")
    @QueryAction
    public Flux<SelectorInfo> getDeviceSelectors() {
        return Flux
            .fromIterable(DeviceSelectorProviders.allProvider())
            //场景联动的设备动作必须选择一个产品,不再列出产品
            .filter(provider -> !"product".equals(provider.getProvider()))
            .map(SelectorInfo::of);
    }

    @Getter
    @Setter
    public static class SelectorInfo {
        @Schema(description = "ID")
        private String id;

        @Schema(description = "名称")
        private String name;

        @Schema(description = "说明")
        private String description;

        public static SelectorInfo of(DeviceSelectorProvider provider) {
            SelectorInfo info = new SelectorInfo();
            info.setId(provider.getProvider());

            info.setName(LocaleUtils
                             .resolveMessage("message.device_selector_" + provider.getProvider(), provider.getName()));

            info.setDescription(LocaleUtils
                                    .resolveMessage("message.device_selector_" + provider.getProvider() + "_desc", provider.getName()));
            return info;
        }
    }

}
