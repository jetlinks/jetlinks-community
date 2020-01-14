package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hswebframework.ezorm.core.param.Term;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.function.Consumer;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class DefaultLinkTypeParser implements LinkTypeParser {

    private TermTypeParser parser = new DefaultTermTypeParser();

    @Override
    public BoolQueryBuilder process(Term term, Consumer<Term> consumer, BoolQueryBuilder queryBuilders) {
        if ("or".equalsIgnoreCase(term.getType().name())) {
            handleOr(queryBuilders, term, consumer);
        } else if ("and".equalsIgnoreCase(term.getType().name())) {
            handleAnd(queryBuilders, term, consumer);
        } else {
            throw new UnsupportedOperationException("不支持的查询连接类型,term.getType:" + term.getType().name());
        }
        return queryBuilders;
    }

    private void handleOr(BoolQueryBuilder queryBuilders, Term term, Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty()) {
            parser.process(() -> term, queryBuilders::should);
        } else {
            BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
            LinkedList<Term> terms = ((LinkedList<Term>) term.getTerms());
            terms.forEach(t -> process(t, consumer, nextQuery));
            queryBuilders.should(nextQuery);
        }
    }

    private void handleAnd(BoolQueryBuilder queryBuilders, Term term, Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty()) {
            parser.process(() -> term, queryBuilders::must);
        } else {
            BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
            LinkedList<Term> terms = ((LinkedList<Term>) term.getTerms());
            terms.forEach(t -> process(t, consumer, nextQuery));
            queryBuilders.must(nextQuery);
        }
    }

    private static Term getLast(LinkedList<Term> terms) {
        int index = terms.indexOf(terms.getLast());
        while (index >= 0) {
            if (terms.get(index).getTerms().isEmpty()) break;
            index--;
        }
        return terms.get(index);
    }
}
