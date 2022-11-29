package org.jetlinks.community.rule.engine.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据规则查询告警配置.
 *
 * 例如：查询场景联动ID为rule-id绑定的告警
 * <pre>
 *     {
 *             "column":"id",
 *             "termType":"rule-bind-alarm",
 *             "value":"rule-id"
 *     }
 * </pre>
 *
 * @author zhangji 2022/11/23
 */
@Component
public class RuleBindAlarmTerm extends AbstractTermFragmentBuilder {

    public RuleBindAlarmTerm() {
        super("rule-bind-alarm", "规则绑定的告警");
    }

    @Override
    public SqlFragments createFragments(String columnFullName,
                                        RDBColumnMetadata column,
                                        Term term) {

        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from ", getTableName("s_alarm_rule_bind", column), " _bind where _bind.alarm_id =", columnFullName);

        List<Object> ruleId = convertList(column, term);
        sqlFragments
            .addSql(
                "and _bind.rule_id in (",
                ruleId.stream().map(r -> "?").collect(Collectors.joining(",")),
                ")")
            .addParameter(ruleId);

        sqlFragments.addSql(")");

        return sqlFragments;
    }
}
