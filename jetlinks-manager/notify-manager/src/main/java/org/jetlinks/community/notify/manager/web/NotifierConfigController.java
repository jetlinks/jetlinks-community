package org.jetlinks.community.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.community.notify.NotifierProvider;
import org.jetlinks.community.notify.NotifyType;
import org.jetlinks.community.notify.manager.entity.NotifyConfigEntity;
import org.jetlinks.community.notify.manager.service.NotifyConfigService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/notifier/config")
@Resource(id = "notifier", name = "通知管理")
@Tag(name = "消息通知配置")
public class NotifierConfigController implements ReactiveServiceCrudController<NotifyConfigEntity, String> {

    private final NotifyConfigService notifyConfigService;

    private final List<NotifierProvider> providers;

    public NotifierConfigController(NotifyConfigService notifyConfigService,
                                    List<NotifierProvider> providers) {
        this.notifyConfigService = notifyConfigService;
        this.providers = providers;
    }


    @Override
    public NotifyConfigService getService() {
        return notifyConfigService;
    }

    @GetMapping("/{type}/{provider}/metadata")
    @QueryAction
    @Operation(summary = "获取指定类型和服务商所需配置定义")
    public Mono<ConfigMetadata> getAllTypes(@PathVariable @Parameter(description = "通知类型ID") String type,
                                            @PathVariable @Parameter(description = "服务商ID") String provider) {
        return Flux.fromIterable(providers)
            .filter(prov -> prov.getType().getId().equalsIgnoreCase(type) && prov.getProvider().getId().equalsIgnoreCase(provider))
            .flatMap(prov -> Mono.justOrEmpty(prov.getNotifierConfigMetadata()))
            .next();
    }


    @GetMapping("/types")
    @QueryAction
    @Operation(summary = "获取平台支持的通知类型")
    public Flux<NotifyTypeInfo> getAllTypes() {
        return Flux.fromIterable(providers)
            .collect(Collectors.groupingBy(NotifierProvider::getType))
            .flatMapIterable(Map::entrySet)
            .map(en -> {
                NotifyTypeInfo typeInfo = new NotifyTypeInfo();
                typeInfo.setId(en.getKey().getId());
                typeInfo.setName(en.getKey().getName());
                typeInfo.setProviderInfos(en.getValue().stream().map(ProviderInfo::of).collect(Collectors.toList()));
                return typeInfo;
            });
    }

    /**
     * 根据类型获取服务商信息
     *
     * @param type 类型标识 {@link NotifyType#getId()}
     * @return 服务商信息
     */
    @GetMapping("/type/{type}/providers")
    @QueryAction
    @Operation(summary = "获取支持的服务商")
    public Flux<ProviderInfo> getTypeProviders(@PathVariable
                                               @Parameter(description = "通知类型ID") String type) {
        return Flux
            .fromIterable(providers)
            .filter(provider -> provider.getType().getId().equals(type))
            .map(ProviderInfo::of);
    }

    @Getter
    @Setter
    @EqualsAndHashCode(of = "id")
    public static class NotifyTypeInfo {
        @Parameter(description = "通知类型ID")
        private String id;

        @Parameter(description = "通知类型名称")
        private String name;

        @Parameter(description = "服务商信息")
        private List<ProviderInfo> providerInfos;

    }

    @AllArgsConstructor
    @Getter
    public static class ProviderInfo {

        @Parameter(description = "通知类型")
        private final String type;

        @Parameter(description = "服务商ID")
        private final String id;

        @Parameter(description = "服务商名称")
        private final String name;

        public static ProviderInfo of(NotifierProvider provider) {
            return new ProviderInfo(provider.getType().getId(), provider.getProvider().getId(), provider.getProvider().getName());
        }

    }

}

