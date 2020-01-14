package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.hswebframework.ezorm.core.param.Term;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @version 1.0
 **/
public interface TermTypeParser {

    void process(Supplier<Term> termSupplier, Function<QueryBuilder, BoolQueryBuilder> function);

}
