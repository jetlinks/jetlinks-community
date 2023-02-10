package org.jetlinks.community.auth.service.terms;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.*;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 根据用户详情条件 查询用户.
 * <p>
 * 将用户详情的条件（手机号、邮箱）嵌套到此条件中
 * <p>
 * "terms": [
 * {
 * "column": "id$user-detail",
 * "value": [
 * {
 * "column": "telephone",
 * "termType": "like",
 * "value": "%888%"
 * },
 * {
 * "column": "email",
 * "termType": "like",
 * "value": "%123%"
 * }
 * ]
 * }
 * ],
 *
 * @author zhangji 2022/6/29
 */
@Component
public class UserDetailTermBuilder extends AbstractTermFragmentBuilder {
    public UserDetailTermBuilder() {
        super("user-detail", "按用户详情查询");
    }

    @SuppressWarnings("all")
    public static List<Term> convertTerms(Object value) {
        if (value instanceof String) {
            String strVal = String.valueOf(value);
            //json字符串
            if (strVal.startsWith("[")) {
                return JSON.parseArray(strVal, Term.class);
            } else {
                //表达式
                return TermExpressionParser.parse(strVal);
            }
        }
        if (value instanceof List) {
            return new JSONArray(((List) value)).toJavaList(Term.class);
        } else {
            throw new UnsupportedOperationException("unsupported term value:" + value);
        }
    }


    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        List<Term> terms = convertTerms(term.getValue());
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from ", getTableName("s_user_detail", column), " _detail where _detail.id = ", columnFullName);

        RDBTableMetadata metadata = column
            .getOwner()
            .getSchema()
            .getTable("s_user_detail")
            .orElseThrow(() -> new UnsupportedOperationException("unsupported s_user_detail"));

        SqlFragments where = builder.createTermFragments(metadata, terms);
        if (!where.isEmpty()) {
            sqlFragments.addSql("and")
                        .addFragments(where);
        }
        sqlFragments.addSql(")");
        return sqlFragments;
    }


    static UserDetailTermsBuilder builder = new UserDetailTermsBuilder();

    static class UserDetailTermsBuilder extends AbstractTermsFragmentBuilder<TableOrViewMetadata> {

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata parameter, List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata table, Term term) {
            if (term.getValue() instanceof NativeSql) {
                NativeSql sql = ((NativeSql) term.getValue());
                return PrepareSqlFragments.of(sql.getSql(), sql.getParameters());
            }
            return table
                .getColumn(term.getColumn())
                .flatMap(column -> table
                    .findFeature(TermFragmentBuilder.createFeatureId(term.getTermType()))
                    .map(termFragment -> termFragment.createFragments(column.getFullName("_detail"), column, term)))
                .orElse(EmptySqlFragments.INSTANCE);
        }
    }
}
