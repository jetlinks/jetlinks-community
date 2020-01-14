package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.hswebframework.ezorm.core.param.Term;

import java.util.function.Consumer;

/**
 * @version 1.0
 **/
public interface LinkTypeParser {

    BoolQueryBuilder process(Term term, Consumer<Term> consumer, BoolQueryBuilder queryBuilders);
}
