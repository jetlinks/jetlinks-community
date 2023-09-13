package org.jetlinks.community.elastic.search.parser;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hswebframework.ezorm.core.param.Term;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
public class DefaultLinkTypeParser implements LinkTypeParser {

    private final TermTypeParser parser = new DefaultTermTypeParser();


    @Override
    public void process(List<Term> terms,
                                    Consumer<Term> consumer,
                                    BoolQueryBuilder queryBuilders) {

        if (CollectionUtils.isEmpty(terms)) {
            return;
        }
        for (TermsHandler.TermGroup group : TermsHandler.groupTerms(terms)) {
            if (group.type == Term.Type.or) {
                for (Term groupTerm : group.getTerms()) {
                    handleOr(queryBuilders, groupTerm, consumer);
                }
            } else {
                BoolQueryBuilder andQuery = QueryBuilders.boolQuery();
                for (Term groupTerm : group.getTerms()) {
                    handleAnd(andQuery, groupTerm, consumer);
                }
                if (!CollectionUtils.isEmpty(andQuery.must())){
                    queryBuilders.should(andQuery);
                }
            }
        }
    }

    private void handleOr(BoolQueryBuilder queryBuilders,
                          Term term,
                          Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty() && term.getValue() != null) {
            parser.process(() -> term, queryBuilders::should);
        } else if (!term.getTerms().isEmpty()){
            BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
            process(term.getTerms(), consumer, nextQuery);
            queryBuilders.should(nextQuery);
        }
    }

    private void handleAnd(BoolQueryBuilder queryBuilders,
                           Term term,
                           Consumer<Term> consumer) {
        consumer.accept(term);
        if (term.getTerms().isEmpty() && term.getValue() != null) {
            parser.process(() -> term, queryBuilders::must);
        } else if (!term.getTerms().isEmpty()){
            BoolQueryBuilder nextQuery = QueryBuilders.boolQuery();
            process(term.getTerms(), consumer, nextQuery);
            queryBuilders.must(nextQuery);
        }
    }

}