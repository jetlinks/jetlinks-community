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
package org.jetlinks.community.device.service;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.api.crud.entity.TreeSupportEntity;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.crud.service.GenericReactiveTreeSupportCrudService;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.DeviceCategoryEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class DeviceCategoryService extends GenericReactiveTreeSupportCrudService<DeviceCategoryEntity, String> implements CommandLineRunner {

    @Override
    public IDGenerator<String> getIDGenerator() {
        return IDGenerator.MD5;
    }

    private static final String category_splitter = "-";

    @Override
    public void setChildren(DeviceCategoryEntity entity, List<DeviceCategoryEntity> children) {
        entity.setChildren(children);
    }

    @Override
    public void run(String... args) {
        this
            .createQuery()
            .fetchOne()
            .switchIfEmpty(initDefaultData().then(Mono.empty()))
            .as(EntityEventHelper::setDoNotFireEvent)
            .subscribe(ignore->{},
                       err -> log.error("init device category error", err));
    }


    static void rebuild(String parentId, List<DeviceCategoryEntity> children) {
        if (children == null) {
            return;
        }
        long sortIndex = 1;
        for (DeviceCategoryEntity child : children) {
            String id = child.getId();
            child.setId(parentId + category_splitter + id + category_splitter);
            child.setParentId(parentId + category_splitter);
            child.setSortIndex(sortIndex++);
            rebuild(parentId + category_splitter + id, child.getChildren());
        }
    }

    private Mono<Void> initDefaultData() {
        return Mono
            .fromCallable(() -> {
                ClassPathResource resource = new ClassPathResource("device-category.json",
                                                                   DeviceCategoryService.class.getClassLoader());

                try (InputStream stream = resource.getInputStream()) {
                    String json = StreamUtils.copyToString(stream, StandardCharsets.UTF_8);

                    List<DeviceCategoryEntity> all = JSON.parseArray(json, DeviceCategoryEntity.class);

                    List<DeviceCategoryEntity> root = TreeSupportEntity.list2tree(all, DeviceCategoryEntity::setChildren);
                    long i = 1;
                    for (DeviceCategoryEntity category : root) {
                        String id = category.getId();
                        category.setId(category_splitter + id + category_splitter);
                        Optional
                            .ofNullable(category.getParentId())
                            .ifPresent(parentId -> {
                                category.setParentId(category_splitter + parentId + category_splitter);
                            });
                        category.setSortIndex(i++);
                        rebuild(category_splitter + id, category.getChildren());
                    }
                    return root;
                }

            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(all -> save(Flux.fromIterable(all)))
            .then();
    }
}
