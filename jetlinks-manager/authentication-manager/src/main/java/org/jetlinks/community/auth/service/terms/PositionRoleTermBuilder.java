package org.jetlinks.community.auth.service.terms;

import org.hswebframework.ezorm.core.Conditional;
import org.hswebframework.ezorm.core.StaticMethodReferenceColumn;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.BatchSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.ezorm.rdb.utils.SqlUtils;
import org.jetlinks.community.utils.ConverterUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

/**
 * 根据岗位ID后查询角色相关信息 or 根据角色查询岗位信息
 */
@Component
public class PositionRoleTermBuilder extends AbstractTermFragmentBuilder {

    public static final String termType = "position-role";

    //基于岗位ID查询关联的角色信息
    public static <E,T extends Conditional<T>> T apply(T c,
                                                     StaticMethodReferenceColumn<E> column,
                                                     Collection<String> positionId) {

        return c.and(column, termType, positionId);
    }

    public PositionRoleTermBuilder() {
        super(termType, "岗位角色关联条件");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        List<String> ids = ConverterUtils.convertToList(term.getValue(), String::valueOf);

        BatchSqlFragments sql = new BatchSqlFragments(5, 1);

        if (term.getOptions().contains("not")) {
            sql.add(SqlFragments.NOT);
        }

        List<String> options = term.getOptions();
        sql.addSql("exists(select 1 from " + getTableName("s_org_position_role", column) + " _r where ");
        if (options.contains("position")) {
            sql.addSql("_r.position_id = ", columnFullName, " and _r.role_id in (");
        } else {
            sql.addSql("_r.role_id = ", columnFullName, " and _r.position_id in (");
        }

        return sql.add(SqlUtils.createQuestionMarks(ids.size()))
                  .addSql("))")
                  .addParameter(ids);
    }
}
