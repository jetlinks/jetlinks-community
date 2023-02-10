package org.jetlinks.community.rule.engine.scene.term;

import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.core.metadata.DataType;

@Deprecated
public interface TermTypeSupport {

    String getType();

    String getName();

    boolean isSupported(DataType type);

    SqlFragments createSql(String column,Object value);

    default TermType type() {
        return TermType.of(getType(), getName());
    }
}
