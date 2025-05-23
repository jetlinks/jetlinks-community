/*
 * Copyright 2025 JetLinks https://www.jetlinks.cn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetlinks.community.terms;

import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.metadata.RDBTableMetadata;
import org.hswebframework.ezorm.rdb.metadata.TableOrViewMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.*;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.jetlinks.community.utils.ConverterUtils;

import java.util.List;

/**
 * 关联子查询动态条件抽象类,统一封装和另外的表进行关联查询的动态条件.
 *
 * <pre>{@code
 *
 * // 动态参数
 * {
 *     "column":"subId",
 *     "value":"name is 123" //支持的格式见: ConverterUtils.convertTerms
 * }
 *
 * exists(
 *       select 1 from {SubTableName} _st where _st.{subTableColumn} = t.sub_id
 *       and _st.name = ?
 *        )
 *
 * }</pre>
 *
 * @author zhouhao
 * @see ConverterUtils#convertTerms(Object)
 * @since 2.2
 */
public abstract class SubTableTermFragmentBuilder extends AbstractTermFragmentBuilder {
    public SubTableTermFragmentBuilder(String termType, String name) {
        super(termType, name);
    }

    static final SqlFragments AND_L = SqlFragments.single("and (");

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        List<Term> terms = ConverterUtils.convertTerms(term.getValue());
        String subTableName = getSubTableName();

        RDBTableMetadata subTable = column
            .getOwner()
            .getSchema()
            .getTable(subTableName, false)
            .orElseThrow(() -> new UnsupportedOperationException("unsupported " + getSubTableName()));

        RDBColumnMetadata subTableColumn = subTable.getColumnNow(getSubTableColumn());

        BatchSqlFragments sqlFragments = new BatchSqlFragments(5, 0);

        if (term.getOptions().contains("not")) {
            sqlFragments.add(SqlFragments.NOT);
        }

        sqlFragments
            .addSql("exists(select 1 from", subTable.getFullName(), getTableAlias(),
                    "where", subTableColumn.getFullName(getTableAlias()), "=", columnFullName);

        SqlFragments where = builder.createTermFragments(subTable, terms);
        if (where.isNotEmpty()) {
            sqlFragments
                .add(AND_L)
                .addFragments(where)
                .add(SqlFragments.RIGHT_BRACKET);
        }
        sqlFragments.add(SqlFragments.RIGHT_BRACKET);
        return sqlFragments;
    }

    /**
     * @return 子表名
     */
    protected abstract String getSubTableName();

    /**
     * @return 子表列名
     */
    protected String getSubTableColumn() {
        return "id";
    }

    /**
     * @return 子查询别名
     */
    protected String getTableAlias() {
        return "_st";
    }

    // 动态条件构造器
    TermsBuilder builder = new TermsBuilder(getTableAlias());

    @AllArgsConstructor
    static class TermsBuilder extends AbstractTermsFragmentBuilder<TableOrViewMetadata> {

        private final String tableAlias;

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata parameter,
                                                   List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }

        @Override
        protected SqlFragments createTermFragments(TableOrViewMetadata table,
                                                   Term term) {
            if (term.getValue() instanceof NativeSql) {
                NativeSql sql = ((NativeSql) term.getValue());
                return SimpleSqlFragments.of(sql.getSql(), sql.getParameters());
            }
            RDBColumnMetadata column = table.getColumn(term.getColumn()).orElse(null);
            if (column == null) {
                return EmptySqlFragments.INSTANCE;
            }

            TermFragmentBuilder builder = column
                .findFeature(TermFragmentBuilder.createFeatureId(term.getTermType()))
                .orElse(null);

            if (builder != null) {
                return builder
                    .createFragments(column.getFullName(tableAlias), column, term);
            }
            return EmptySqlFragments.INSTANCE;
        }
    }
}
