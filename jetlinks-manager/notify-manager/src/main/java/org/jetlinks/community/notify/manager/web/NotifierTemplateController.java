package org.jetlinks.community.notify.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.community.notify.manager.entity.NotifyTemplateEntity;
import org.jetlinks.community.notify.manager.service.NotifyConfigService;
import org.jetlinks.community.notify.manager.service.NotifyTemplateService;
import org.jetlinks.community.notify.manager.web.response.TemplateInfo;
import org.jetlinks.community.notify.template.TemplateProvider;
import org.jetlinks.community.notify.template.VariableDefinition;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wangzheng
 * @author zhouhao
 * @since 1.0
 */
@RestController
@RequestMapping("/notifier/template")
@Authorize
@Resource(id = "template", name = "通知模板")
@Tag(name = "消息通知模版")
public class NotifierTemplateController implements ReactiveServiceCrudController<NotifyTemplateEntity, String> {

    private final NotifyTemplateService templateService;

    private final List<TemplateProvider> providers;
    private final NotifyConfigService configService;

    public NotifierTemplateController(NotifyTemplateService templateService,
                                      NotifyConfigService configService,
                                      List<TemplateProvider> providers) {
        this.templateService = templateService;
        this.providers = providers;
        this.configService = configService;
    }

    @Override
    public ReactiveCrudService<NotifyTemplateEntity, String> getService() {
        return templateService;
    }

    @PostMapping("/{configId}/_query")
    @QueryAction
    @Operation(summary = "根据配置ID查询通知模版列表")
    public Flux<NotifyTemplateEntity> queryTemplatesByConfigId(@PathVariable
                                                               @Parameter(description = "配置ID") String configId,
                                                               @RequestBody Mono<QueryParamEntity> query) {
        return configService
            .findById(configId)
            .flatMapMany(conf -> query
                .flatMapMany(param -> param
                    .toNestQuery(nest -> nest
                        //where type = ? and provider = ? and (config_id = ? or config_id is null or config_id = '')
                        .is(NotifyTemplateEntity::getType, conf.getType())
                        .is(NotifyTemplateEntity::getProvider, conf.getProvider())
                        .nest()
                        /**/.is(NotifyTemplateEntity::getConfigId, configId)
                        /*  */.or()
                        /**/.isNull(NotifyTemplateEntity::getConfigId)
                        .isEmpty(NotifyTemplateEntity::getConfigId)
                    )
                    .noPaging()
                    .execute(templateService::query)));

    }

    @PostMapping("/{configId}/detail/_query")
    @QueryAction
    @Operation(summary = "根据配置ID查询通知模版详情列表")
    public Flux<NotifyTemplateEntity> queryTemplatesDetailByConfigId(@PathVariable
                                                                     @Parameter(description = "配置ID") String configId,
                                                                     @RequestBody Mono<QueryParamEntity> query) {
        return configService
            .findById(configId)
            .flatMapMany(conf -> query
                .flatMapMany(param -> param
                    .toNestQuery(nest -> nest
                        //where type = ? and provider = ? and (config_id = ? or config_id is null or config_id = '')
                        .is(NotifyTemplateEntity::getType, conf.getType())
                        .is(NotifyTemplateEntity::getProvider, conf.getProvider())
                        .nest()
                        /**/.is(NotifyTemplateEntity::getConfigId, configId)
                        /*  */.or()
                        /**/.isNull(NotifyTemplateEntity::getConfigId)
                        .isEmpty(NotifyTemplateEntity::getConfigId)
                    )
                    .noPaging()
                    .execute(templateService::query)))
            .flatMap(e -> this
                .convertVariableDefinitions(e)
                .doOnNext(e::setVariableDefinitions)
                .thenReturn(e));

    }

    @GetMapping("/{templateId}/detail")
    @QueryAction
    @Operation(summary = "获取模版详情信息")
    public Mono<TemplateInfo> getTemplateDetail(@PathVariable
                                                @Parameter(description = "模版ID") String templateId) {
        return templateService
            .findById(templateId)
            .flatMap(e -> {
                TemplateInfo info = new TemplateInfo();
                info.setId(e.getId());
                info.setName(e.getName());
                return this
                    .getProvider(e.getType(), e.getProvider())
                    .createTemplate(e.toTemplateProperties())
                    .doOnNext(t -> info.setVariableDefinitions(new ArrayList<>(t.getVariables().values())))
                    .thenReturn(info);
            });
    }


    @GetMapping("/{type}/{provider}/config/metadata")
    @QueryAction
    @Operation(summary = "获取指定类型和服务商所需模版配置定义")
    public Mono<ConfigMetadata> getConfigMetadata(@PathVariable @Parameter(description = "通知类型ID") String type,
                                                  @PathVariable @Parameter(description = "服务商ID") String provider) {
        return Mono.justOrEmpty(getProvider(type, provider).getTemplateConfigMetadata());
    }

    public TemplateProvider getProvider(String type, String provider) {
        for (TemplateProvider prov : providers) {
            if (prov.getType().getId().equalsIgnoreCase(type) && prov
                .getProvider()
                .getId()
                .equalsIgnoreCase(provider)) {
                return prov;
            }
        }
        throw new ValidationException("error.unsupported_notify_provider");
    }

    private Mono<List<VariableDefinition>> convertVariableDefinitions(NotifyTemplateEntity templateEntity) {
        return this
            .getProvider(templateEntity.getType(), templateEntity.getProvider())
            .createTemplate(templateEntity.toTemplateProperties())
            .map(t -> new ArrayList<>(t.getVariables().values()));
    }

}
