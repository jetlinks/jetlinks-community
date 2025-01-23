package org.jetlinks.community.reactorql.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.BatchSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.community.reactorql.impl.ComplexExistsFunction;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.ArrayType;

import java.util.function.BiFunction;

/**
 * @author zhangji 2025/1/23
 * @since 2.3
 */
public class ExistsTermSupport implements TermTypeSupport {
    static {
        ComplexExistsFunction.register();
    }

    @Override
    public String getType() {
        return "complex_exists";
    }

    @Override
    public String getName() {
        return "满足";
    }

    @Override
    public boolean isSupported(DataType type) {
        return type instanceof ArrayType;
    }

    @Override
    public Term refactorTerm(String tableName,
                             Term term,
                             BiFunction<String, Term, Term> refactor) {
        Term t = refactor.apply(tableName, term);
        ComplexExistsFunction.ExistsSpec existsSpec = ComplexExistsFunction.createExistsSpec(t.getValue());

        existsSpec.walkTerms(__term -> {

            String col = __term.getColumn();
            //使用 _row 获取原始行数据
            refactor.apply(ComplexExistsFunction.COL_ROW, __term);
            //由原始条件指定的列名为准,如: _element.this 、_element.num
            __term.setColumn(col);

        });

        t.setValue(existsSpec);

        return t;
    }

    @Override
    public SqlFragments createSql(String column, Object value, Term term) {

        ComplexExistsFunction.ExistsSpec existsSpec = ComplexExistsFunction.createExistsSpec(value);

        BatchSqlFragments fragments = new BatchSqlFragments();

        fragments
            .addSql("complex_exists(?,", column, ")")
            .addParameter(existsSpec.compile());


        return fragments;
    }

}
