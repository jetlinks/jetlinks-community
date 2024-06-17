package org.jetlinks.community.rule.engine.service.terms;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.community.utils.ObjectMappers;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 根据规则查询告警配置.
 * <p>
 * 例如：查询场景联动ID为rule-id绑定的告警
 * <pre>{@code
 *     {
 *             "column":"id",
 *             "termType":"rule-bind-alarm",
 *             "value":"rule-id"
 *     }
 *
 *     {
 *             "column":"id",
 *             "termType":"rule-bind-alarm",
 *             "value": {
 *                 "ruleId":["rule-id"],
 *                 "branchId":[-1,2]
 *             }
 *     }
 * }</pre>
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

        AlarmRuleBindTerm bindTerm = AlarmRuleBindTerm.of(term.getValue());
        if (CollectionUtils.isEmpty(bindTerm.ruleId)) {
            throw new IllegalArgumentException("illegal term [rule-bind-alarm] value :" + term);
        }
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from ", getTableName("s_alarm_rule_bind", column), " _bind where _bind.alarm_id =", columnFullName);

        sqlFragments
            .addSql(
                "and _bind.rule_id in (",
                bindTerm.ruleId.stream().map(r -> "?").collect(Collectors.joining(",")),
                ")")
            .addParameter(bindTerm.ruleId);

        if (CollectionUtils.isNotEmpty(bindTerm.branchId)) {
            sqlFragments
                .addSql(
                    "and _bind.branch_index in (",
                    bindTerm.branchId.stream().map(r -> "?").collect(Collectors.joining(",")),
                    ")")
                .addParameter(bindTerm.branchId);
        }

        sqlFragments.addSql(")");

        return sqlFragments;
    }

    @Getter
    @Setter
    public static class AlarmRuleBindTerm {
        private List<String> ruleId;

        private List<Integer> branchId;

        public static AlarmRuleBindTerm of(Object term) {
            if(term instanceof AlarmRuleBindTerm){
                return ((AlarmRuleBindTerm) term);
            }
            AlarmRuleBindTerm bindTerm = new AlarmRuleBindTerm();

            if (term instanceof String) {
                String str = ((String) term);
                if (str.startsWith("{")) {
                    term = ObjectMappers.parseJson(str, Map.class);
                } else if (str.startsWith("[")) {
                    term = ObjectMappers.parseJsonArray(str, Object.class);
                } else if (str.contains(":")) {
                    // ruleId:-1,1,2,3
                    String[] split = str.split(":");
                    bindTerm.setRuleId(Collections.singletonList(split[0]));
                    bindTerm.setBranchId(ConverterUtils.convertToList(split[1], val -> CastUtils
                        .castNumber(val)
                        .intValue()));
                } else {
                    bindTerm.setRuleId(Collections.singletonList(str));
                }
            }

            if (term instanceof Collection) {
                bindTerm.setRuleId(ConverterUtils.convertToList(term, String::valueOf));
            }

            if (term instanceof Map) {
                FastBeanCopier.copy(term, bindTerm);
            }


            return bindTerm;
        }
    }

}
