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
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.dictionary.entity.DictionaryItemEntity;
import org.hswebframework.web.dictionary.service.DefaultDictionaryItemService;
import org.springframework.boot.CommandLineRunner;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author bestfeng
 */
@AllArgsConstructor
public class DatabaseDictionaryManager implements DictionaryManager, CommandLineRunner{

    private final DefaultDictionaryItemService dictionaryItemService;

    private final Map<String, Map<String, DictionaryItemEntity>> itemStore = new ConcurrentHashMap<>();

    @Nonnull
    @Override
    public List<EnumDict<?>> getItems(@Nonnull String dictId) {
        Map<String, DictionaryItemEntity> itemEntityMap = itemStore.get(dictId);
        if (MapUtils.isEmpty(itemEntityMap)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(itemEntityMap.values());
    }

    @Nonnull
    @Override
    public Optional<EnumDict<?>> getItem(@Nonnull String dictId, @Nonnull String itemId) {
        Map<String, DictionaryItemEntity> itemEntityMap = itemStore.get(dictId);
        if (itemEntityMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(itemEntityMap.get(itemId));
    }


    public void registerItems(List<DictionaryItemEntity> items) {
        items.forEach(this::registerItem);
    }


    public void removeItems(List<DictionaryItemEntity> items) {
        items.forEach(this::removeItem);
    }


    public void removeItem(DictionaryItemEntity item) {
        if (item == null || item.getDictId() == null || item.getId() == null) {
            return;
        }
        itemStore.compute(item.getDictId(), (k, v) -> {
            if (v != null) {
                v.remove(item.getId());
                if (!v.isEmpty()) {
                    return v;
                }
            }
            return null;
        });
    }


    public void registerItem(DictionaryItemEntity item) {
        if (item == null || item.getDictId() == null) {
            return;
        }
        itemStore
            .computeIfAbsent(item.getDictId(), k -> new ConcurrentHashMap<>())
            .put(item.getId(), item);
    }

    @Override
    public void run(String... args) throws Exception {
        dictionaryItemService
            .createQuery()
            .fetch()
            .doOnNext(this::registerItem)
            .subscribe();
    }
}
