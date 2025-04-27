package org.jetlinks.community.elastic.search.enums;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.community.spi.Provider;
import org.jetlinks.community.things.utils.ThingsDatabaseUtils;

public interface ElasticSearchTermType {

    Provider<ElasticSearchTermType> supports = Provider.create(ElasticSearchTermType.class);

    String getId();

    boolean isSupported(Term term);

    Query.Builder process(Term term, Query.Builder builder);

    default Query process(Term term) {
        return process(term, new Query.Builder()).build();
    }

    default Object convertTermValue(DataType type, Object value) {
        return ThingsDatabaseUtils.tryConvertTermValue(type, value);
    }
}
