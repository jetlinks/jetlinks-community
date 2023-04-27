package org.jetlinks.community.reactorql.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.core.metadata.DataType;

public interface TermTypeSupport {

    String getType();

    String getName();

    boolean isSupported(DataType type);

    SqlFragments createSql(String column, Object value, Term term);

    default TermType type() {
        return TermType.of(getType(), getName());
    }
}
