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
package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.QueryNoPagingOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.jetlinks.community.device.entity.DeviceCategoryEntity;
import org.jetlinks.community.device.service.DeviceCategoryService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/device/category")
@Slf4j
@Tag(name = "产品分类管理")
@AllArgsConstructor
@Resource(id="device-category",name = "产品分类")
public class DeviceCategoryController implements ReactiveServiceCrudController<DeviceCategoryEntity,String> {


    private final DeviceCategoryService categoryService;

    @GetMapping
    @QueryNoPagingOperation(summary = "获取全部分类")
    @Authorize(merge = false)
    public Flux<DeviceCategoryEntity> getAllCategory(@Parameter(hidden = true) QueryParamEntity query) {
        return this
            .categoryService
            .createQuery()
            .setParam(query)
            .fetch();
    }

    @GetMapping("/_tree")
    @QueryNoPagingOperation(summary = "获取全部分类(树结构)")
    @Authorize(merge = false)
    public Flux<DeviceCategoryEntity> getAllCategoryTree(@Parameter(hidden = true) QueryParamEntity query) {
        return this
            .categoryService
            .createQuery()
            .setParam(query)
            .fetch()
            .collectList()
            .flatMapMany(all-> Flux.fromIterable(TreeSupportEntity.list2tree(all, DeviceCategoryEntity::setChildren)));
    }


    @PostMapping("/_tree")
    @QueryNoPagingOperation(summary = "获取全部分类(树结构)")
    @Authorize(merge = false)
    public Flux<DeviceCategoryEntity> getAllCategoryTreeByQueryParam(@RequestBody Mono<QueryParamEntity> query) {
        return this
                .categoryService
                .query(query)
                .collectList()
                .flatMapMany(all-> Flux.fromIterable(TreeSupportEntity.list2tree(all, DeviceCategoryEntity::setChildren)));
    }

    @Override
    public ReactiveCrudService<DeviceCategoryEntity, String> getService() {
        return categoryService;
    }
}
