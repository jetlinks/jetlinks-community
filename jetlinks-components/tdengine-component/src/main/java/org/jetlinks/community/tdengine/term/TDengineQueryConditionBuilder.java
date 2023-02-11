package org.jetlinks.community.tdengine.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.AbstractTermsFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class TDengineQueryConditionBuilder extends AbstractTermsFragmentBuilder<Object> {


    public static String build(List<Term> terms) {

        if(CollectionUtils.isEmpty(terms)){
            return "";
        }
        SqlFragments fragments = new TDengineQueryConditionBuilder().createTermFragments(null, terms);

        if(fragments.isEmpty()){
            return "";
        }

        return fragments.toRequest().toString();

    }

    @Override
    protected SqlFragments createTermFragments(Object parameter, Term term) {
        String type = term.getTermType();
        TDengineTermType termType = TDengineTermType.valueOf(type.toLowerCase());

        return SqlFragments.single(termType.build("`"+term.getColumn()+"`", term.getValue()));
    }
}
