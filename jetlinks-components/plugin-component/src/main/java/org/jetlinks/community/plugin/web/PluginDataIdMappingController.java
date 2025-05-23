/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.plugin.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.jetlinks.community.plugin.impl.id.PluginDataIdMappingEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 插件数据ID映射.
 *
 * @author zhangji 2023/3/2
 */
@AllArgsConstructor
@RestController
@RequestMapping("/plugin/mapping")
@Resource(id = "plugin-driver", name = "插件驱动管理")
@Tag(name = "插件数据ID映射")
public class PluginDataIdMappingController {

    private final ReactiveRepository<PluginDataIdMappingEntity, String> repository;

    @PatchMapping("/{type}/{pluginId:.+}/{internalId:.+}")
    @SaveAction
    @Operation(summary = "保存数据ID映射")
    public Mono<SaveResult> save(@PathVariable @Parameter(description = "插件数据类型") String type,
                                 @PathVariable @Parameter(description = "插件ID") String pluginId,
                                 @PathVariable @Parameter(description = "内部数据ID") String internalId,
                                 @RequestBody Mono<String> externalId) {
        return externalId
            .map(id -> PluginDataIdMappingEntity.of(pluginId, internalId, type, id))
            .flatMap(entity -> repository
                .createDelete()
                .where(PluginDataIdMappingEntity::getPluginId, pluginId)
                .and(PluginDataIdMappingEntity::getInternalId, internalId)
                .and(PluginDataIdMappingEntity::getType, type)
                .execute()
                .thenReturn(entity))
            .flatMap(repository::save);
    }

    @GetMapping("/{type}/{pluginId:.+}/{internalId:.+}")
    @QueryAction
    @Operation(summary = "获取数据ID映射")
    public Mono<PluginDataIdMappingEntity> queryOne(@PathVariable @Parameter(description = "插件数据类型") String type,
                                                    @PathVariable @Parameter(description = "插件ID") String pluginId,
                                                    @PathVariable @Parameter(description = "内部数据ID") String internalId) {
        return repository
            .createQuery()
            .where(PluginDataIdMappingEntity::getPluginId, pluginId)
            .and(PluginDataIdMappingEntity::getInternalId, internalId)
            .and(PluginDataIdMappingEntity::getType, type)
            .fetchOne();
    }

    @PostMapping("/{type}/_all")
    @QueryAction
    @Operation(summary = "获取指定类型的所有ID映射")
    @Deprecated
    public Flux<PluginDataIdMappingEntity> queryAll(@PathVariable @Parameter(description = "插件数据类型") String type,
                                                    @RequestBody(required = false)
                                                    @Parameter(description = "指定查询的插件数据ID") Mono<List<String>> includes) {
        return includes
            .flatMapMany(externalIds -> repository
                .createQuery()
                .where(PluginDataIdMappingEntity::getType, type)
                .when(!CollectionUtils.isEmpty(externalIds),
                      query -> query.in(PluginDataIdMappingEntity::getExternalId, externalIds))
                .fetch());
    }

    @PostMapping("/{type}/{pluginId:.+}/_all")
    @QueryAction
    @Operation(summary = "获取指定类型的所有ID映射")
    public Flux<PluginDataIdMappingEntity> queryAll(@PathVariable @Parameter(description = "插件数据类型") String type,
                                                    @PathVariable @Parameter(description = "插件ID") String pluginId,
                                                    @RequestBody(required = false)
                                                    @Parameter(description = "指定查询的插件数据ID") Mono<List<String>> includes) {
        return includes
            .flatMapMany(externalIds -> repository
                .createQuery()
                .where(PluginDataIdMappingEntity::getType, type)
                .and(PluginDataIdMappingEntity::getPluginId, pluginId)
                .when(!CollectionUtils.isEmpty(externalIds),
                    query -> query.in(PluginDataIdMappingEntity::getExternalId, externalIds))
                .fetch());
    }

    @PostMapping("/_query")
    @QueryAction
    @Operation(summary = "动态查询ID映射")
    public Flux<PluginDataIdMappingEntity> query(@RequestBody(required = false) Mono<QueryParamEntity> queryParam) {
        return queryParam
                .flatMapMany(query -> repository
                        .createQuery()
                        .setParam(query)
                        .fetch());
    }
}
