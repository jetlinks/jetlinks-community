package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.hswebframework.ezorm.core.param.Term;

import java.util.List;
import java.util.function.Consumer;

/**
 * @version 1.0
 **/
public interface LinkTypeParser {

    void process(List<Term> terms, Consumer<Term> consumer, BoolQueryBuilder queryBuilders);
}
