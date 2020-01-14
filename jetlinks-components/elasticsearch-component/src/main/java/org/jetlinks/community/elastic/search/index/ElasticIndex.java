package org.jetlinks.community.elastic.search.index;

import java.util.function.Supplier;

/**
 * @version 1.0
 **/
public interface ElasticIndex {

    @Deprecated
    String getIndex();

    @Deprecated
    String getType();

    default String getStandardIndex(){
        return getIndex().toLowerCase();
    }

    default String getStandardType(){
        return getType().toLowerCase();
    }

    static ElasticIndex createDefaultIndex(Supplier<String> indexConsumer, Supplier<String> typeConsumer) {
        return new ElasticIndex() {
            @Override
            public String getIndex() {
                return indexConsumer.get();
            }

            @Override
            public String getType() {
                return typeConsumer.get();
            }
        };
    }
}
