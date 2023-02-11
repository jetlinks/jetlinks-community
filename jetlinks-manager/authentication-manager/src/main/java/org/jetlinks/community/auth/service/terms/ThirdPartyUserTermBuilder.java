package org.jetlinks.community.auth.service.terms;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.web.exception.I18nSupportException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * <pre>{@code
 *
 *   where id$user-third$type = provider
 *
 * }</pre>
 */
@Component
public class ThirdPartyUserTermBuilder extends AbstractTermFragmentBuilder {

    public ThirdPartyUserTermBuilder() {
        super("user-third", "第三方用户绑定信息");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from ",getTableName("s_third_party_user_bind",column)," _bind where _bind.user_id =", columnFullName);

        if (CollectionUtils.isEmpty(term.getOptions())) {
            throw new I18nSupportException("error.query_conditions_are_not_specified", column.getName()+"$user-third$type");
        }
        String type = term.getOptions().get(0);

        List<Object> idList = convertList(column, term);
        String[] args = new String[idList.size()];
        Arrays.fill(args, "?");
        sqlFragments
            .addSql(
                "and _bind.type = ? and _bind.provider in (", String.join(",", args), ")")
            .addParameter(type)
            .addParameter(idList);

        sqlFragments.addSql(")");

        return sqlFragments;
    }
}
