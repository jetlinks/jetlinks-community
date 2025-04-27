package org.jetlinks.community.elastic.search.enums;

import org.hswebframework.ezorm.core.param.Term;

import java.util.Optional;

public class ElasticSearchTermTypes {


    static {

    }

    public static void register(ElasticSearchTermType termType) {
        ElasticSearchTermType.supports.register(termType.getId(), termType);
    }

    public static ElasticSearchTermType lookupNow(Term term) {

        return lookup(term)
            .orElseThrow(() -> new UnsupportedOperationException("不支持的查询条件:" + term.getType()));
    }

    public static Optional<ElasticSearchTermType> lookup(Term term) {
        ElasticSearchTermType fast = ElasticSearchTermType
            .supports
            .get(term.getTermType())
            .orElse(null);
        if (fast != null) {
            return Optional.of(fast);
        }

        for (ElasticSearchTermType termType : ElasticSearchTermType.supports.getAll()) {
            if (termType.isSupported(term)) {
                return Optional.of(termType);
            }
        }
        return Optional.empty();
    }

}
