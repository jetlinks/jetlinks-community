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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.dictionary.entity.DictionaryEntity;
import org.hswebframework.web.dictionary.entity.DictionaryItemEntity;
import org.hswebframework.web.dictionary.service.DefaultDictionaryItemService;
import org.hswebframework.web.dictionary.service.DefaultDictionaryService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * @author gyl
 * @since 2.2
 */
@Slf4j
public class DictionaryInitManager implements CommandLineRunner {

    @Getter
    @Setter
    private List<DictionaryEntity> inits = new ArrayList<>();

    public final ObjectProvider<DictionaryInitInfo> initInfo;

    private final DefaultDictionaryService defaultDictionaryService;

    private final DefaultDictionaryItemService itemService;

    public DictionaryInitManager(ObjectProvider<DictionaryInitInfo> initInfo, DefaultDictionaryService defaultDictionaryService, DefaultDictionaryItemService itemService) {
        this.initInfo = initInfo;
        this.defaultDictionaryService = defaultDictionaryService;
        this.itemService = itemService;
    }

    @Override
    public void run(String... args) {
        Flux
            .merge(
                Flux.fromIterable(inits),
                Flux
                    .fromIterable(initInfo)
                    .flatMap(DictionaryInitInfo::getDictAsync)
            )
            .buffer(200)
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(collection -> {
                List<DictionaryItemEntity> items = generateItems(collection);
                return defaultDictionaryService
                    .save(collection)
                    .mergeWith(itemService.save(items));
            })
            .as(EntityEventHelper::setDoNotFireEvent)
            .subscribe(ignore -> {
                       },
                       err -> log.error("init dict error", err));

    }


    public List<DictionaryItemEntity> generateItems(List<DictionaryEntity> dictionaryList) {
        List<DictionaryItemEntity> items = new ArrayList<>();
        for (DictionaryEntity dictionary : dictionaryList) {
            if (!CollectionUtils.isEmpty(dictionary.getItems())) {
                for (DictionaryItemEntity item : dictionary.getItems()) {
                    item.setDictId(dictionary.getId());
                    items.add(item);
                }
            }
        }
        return items;
    }


}
