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
package org.jetlinks.community.dictionary;

import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.dictionary.entity.DictionaryEntity;
import org.hswebframework.web.dictionary.entity.DictionaryItemEntity;
import org.hswebframework.web.dictionary.service.DefaultDictionaryItemService;
import org.hswebframework.web.dictionary.service.DefaultDictionaryService;
import org.hswebframework.web.exception.BusinessException;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

/**
 * @author bestfeng
 */
@AllArgsConstructor
public class DictionaryEventHandler {


    private final DefaultDictionaryItemService itemService;

    @EventListener
    public void handleDictionaryCreated(EntityCreatedEvent<DictionaryEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(dictionary -> {
                    if (!CollectionUtils.isEmpty(dictionary.getItems())) {
                        return Flux
                            .fromIterable(dictionary.getItems())
                            .doOnNext(item -> item.setDictId(dictionary.getId()))
                            .as(itemService::save);
                    }
                    return Mono.empty();
                })
        );

    }

    @EventListener
    public void handleDictionarySaved(EntitySavedEvent<DictionaryEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .flatMap(dictionary -> {
                    if (!CollectionUtils.isEmpty(dictionary.getItems())) {
                        return Flux
                            .fromIterable(dictionary.getItems())
                            .doOnNext(item -> item.setDictId(dictionary.getId()))
                            .as(itemService::save);
                    }
                    return Mono.empty();
                })
        );
    }

    @EventListener
    public void handleDictionaryDeleted(EntityDeletedEvent<DictionaryEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .map(DictionaryEntity::getId)
                .collectList()
                .flatMap(dictionary -> itemService
                    .createDelete()
                    .where()
                    .in(DictionaryItemEntity::getDictId, dictionary)
                    .execute()
                    .then())
        );
    }

    /**
     * 监听字典删除前事件，阻止删除分类标识为系统的字典
     *
     * @param event 字典删除前事件
     */
    @EventListener
    public void handleDictionaryBeforeDelete(EntityBeforeDeleteEvent<DictionaryEntity> event) {
        event.async(
            Flux.fromIterable(event.getEntity())
                .any(dictionary ->
                         StringUtils.equals(dictionary.getClassified(), DictionaryConstants.CLASSIFIED_SYSTEM))
                .flatMap(any -> {
                    if (any) {
                        return Mono.error(() -> new BusinessException("error.system_dictionary_can_not_delete"));
                    }
                    return Mono.empty();
                })
        );
    }
}
