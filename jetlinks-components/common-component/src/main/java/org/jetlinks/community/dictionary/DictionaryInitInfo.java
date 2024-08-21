package org.jetlinks.community.dictionary;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.web.dictionary.entity.DictionaryEntity;
import reactor.core.publisher.Flux;

import java.util.Collection;

/**
 * @author gyl
 * @since 2.2
 */
public interface DictionaryInitInfo {
    Collection<DictionaryEntity> getDict();

    default Flux<DictionaryEntity> getDictAsync() {
        if (CollectionUtils.isEmpty(getDict())) {
            return Flux.empty();
        }
        return Flux.fromIterable(getDict());
    }

}
