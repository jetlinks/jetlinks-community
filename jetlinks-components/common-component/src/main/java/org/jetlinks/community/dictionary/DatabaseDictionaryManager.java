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
