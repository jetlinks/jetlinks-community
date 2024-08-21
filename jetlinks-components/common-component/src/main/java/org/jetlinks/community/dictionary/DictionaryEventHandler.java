package org.jetlinks.community.dictionary;

import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.crud.events.*;
import org.hswebframework.web.dictionary.entity.DictionaryEntity;
import org.hswebframework.web.dictionary.entity.DictionaryItemEntity;
import org.hswebframework.web.dictionary.service.DefaultDictionaryItemService;
import org.hswebframework.web.exception.BusinessException;
import org.springframework.context.event.EventListener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author bestfeng
 */
@AllArgsConstructor
public class DictionaryEventHandler implements EntityEventListenerCustomizer {

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

    @Override
    public void customize(EntityEventListenerConfigure configure) {
        configure.enable(DictionaryItemEntity.class);
        configure.enable(DictionaryEntity.class);
    }

    /**
     * 监听字典删除前事件，阻止删除分类标识为系统的字典
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
