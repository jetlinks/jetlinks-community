package org.jetlinks.community.elastic.search.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.elastic.search.utils.TermCommonUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Jia_RG
 * @author bestfeng
 */
@Getter
@AllArgsConstructor
public enum TermTypeEnum {
    eq("eq") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.termQuery(term.getColumn().trim(), term.getValue());
        }
    },
    not("not") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.termQuery(term.getColumn().trim(), term.getValue()));
        }
    },
    btw("btw") {
        @Override
        public QueryBuilder process(Term term) {
            Object between = null;
            Object and = null;
            List<?> values = TermCommonUtils.convertToList(term.getValue());
            if (values.size() > 0) {
                between = values.get(0);
            }
            if (values.size() > 1) {
                and = values.get(1);
            }
            return QueryBuilders.rangeQuery(term.getColumn().trim()).gte(between).lte(and);
        }
    },
    gt("gt") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.rangeQuery(term.getColumn().trim()).gt(term.getValue());
        }
    },
    gte("gte") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.rangeQuery(term.getColumn().trim()).gte(term.getValue());
        }
    },
    lt("lt") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.rangeQuery(term.getColumn().trim()).lt(term.getValue());
        }
    },
    lte("lte") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.rangeQuery(term.getColumn().trim()).lte(term.getValue());
        }
    },
    in("in") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.termsQuery(term.getColumn().trim(), TermCommonUtils.convertToList(term.getValue()));
        }
    },
    like("like") {
        @Override
        public QueryBuilder process(Term term) {
            //return QueryBuilders.matchPhraseQuery(term.getColumn().trim(), term.getValue());
            return QueryBuilders.wildcardQuery(term.getColumn().trim(), likeQueryTermValueHandler(term.getValue()));
        }
    },
    nlike("nlike") {
        @Override
        public QueryBuilder process(Term term) {
            return QueryBuilders.boolQuery().mustNot(QueryBuilders.wildcardQuery(term.getColumn().trim(), likeQueryTermValueHandler(term.getValue())));
        }
    };

    private final String type;

    public abstract QueryBuilder process(Term term);

    public static String likeQueryTermValueHandler(Object value) {
        if (!StringUtils.isEmpty(value)) {
            return value.toString().replace("%", "*");
        }
        return "**";
    }

    public static Optional<TermTypeEnum> of(String type) {
        return Arrays.stream(values())
                .filter(e -> e.getType().equalsIgnoreCase(type))
                .findAny();
    }
}
