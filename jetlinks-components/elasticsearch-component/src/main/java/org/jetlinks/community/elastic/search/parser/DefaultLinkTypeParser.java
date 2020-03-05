package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hswebframework.ezorm.core.param.Term;
import org.springframework.stereotype.Component;

import java.util.List;
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
        if (term.getType() == Term.Type.or) {
            handleOr(queryBuilders, term, consumer);
        } else {
            handleAnd(queryBuilders, term, consumer);
        }
        return queryBuilders;
    }

    private void handleOr(BoolQueryBuilder queryBuilders, Term term, Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty() && term.getValue() != null) {
            parser.process(() -> term, queryBuilders::should);
        } else {
            BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
            List<Term> terms = (term.getTerms());
            terms.forEach(t -> process(t, consumer, nextQuery));
            queryBuilders.should(nextQuery);
        }
    }

    private void handleAnd(BoolQueryBuilder queryBuilders, Term term, Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty() && term.getValue() != null) {
            parser.process(() -> term, queryBuilders::must);
        } else {
            BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
            List<Term> terms = term.getTerms();
            terms.forEach(t -> process(t, consumer, nextQuery));
            queryBuilders.must(nextQuery);
        }
    }


}
