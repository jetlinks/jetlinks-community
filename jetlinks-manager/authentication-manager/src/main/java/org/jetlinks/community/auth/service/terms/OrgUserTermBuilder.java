package org.jetlinks.community.auth.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.BatchSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.ezorm.rdb.utils.SqlUtils;
import org.jetlinks.community.auth.dimension.OrgDimensionType;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询组织关联的用户.
 *
 * 只查询组织绑定的用户：
 * <pre>{@code
 *     "terms":[
 *         {
 *             "column":"id$in-org-user$org",
 *             "value":["orgId"]
 *         }
 *     ]
 * }</pre>
 *
 *
 * 查询组织或职位绑定的用户
 * <pre>{@code
 *     "terms":[
 *         {
 *             "column":"id$in-org-user",
 *             "value":["orgId"]
 *         }
 *     ]
 * }</pre>
 *
 * @author zhangji 2025/2/12
 * @since 2.3
 */
@Component
public class OrgUserTermBuilder extends AbstractTermFragmentBuilder {
    public static final String termType = "in-org-user";

    public OrgUserTermBuilder() {
        super(termType, "组织关联的所有用户");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        List<Object> values = convertList(column, term);

        BatchSqlFragments fragments = new BatchSqlFragments(15, 4);
        List<String> options = term.getOptions();

        if (options.contains("not")) {
            fragments.add(SqlFragments.NOT);
        }

        // 组织绑定条件，options未指定时也查询
        if (options.contains("org")) {
            fragments.addSql("exists(select 1 from",
                             getTableName("s_dimension_user", column),
                             "d where d.user_id =", columnFullName,
                             "and (");
            fragments.addSql("d.dimension_type_id = ?")
                     .addParameter(OrgDimensionType.org.getId());
            if (!options.contains("any")) {
                fragments
                    .addSql("and d.dimension_id in(")
                    .add(SqlUtils.createQuestionMarks(values.size()))
                    .add(SqlFragments.RIGHT_BRACKET)
                    .addParameter(values);
            }
        } else {
            return fragments;
        }

        fragments.add(SqlFragments.RIGHT_BRACKET)
                 .add(SqlFragments.RIGHT_BRACKET);

        return fragments;
    }
}
