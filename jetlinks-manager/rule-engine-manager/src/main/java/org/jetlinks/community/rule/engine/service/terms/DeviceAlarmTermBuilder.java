package org.jetlinks.community.rule.engine.service.terms;

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
 * 根据告警记录查询设备相关数据，可以使用 {@link org.jetlinks.community.rule.engine.entity.DeviceAlarmHistoryEntity}的属性作为关联条件
 * <p>
 * where id dev-alarm 'state not xxx'
 *
 * @author zhouhao
 * @see org.jetlinks.community.rule.engine.entity.DeviceAlarmHistoryEntity
 * @since 1.12
 */
@Component
public class DeviceAlarmTermBuilder extends AbstractTermFragmentBuilder {

    public DeviceAlarmTermBuilder() {
        super("dev-alarm", "根据告警查询设备相关数据");
    }

    @SuppressWarnings("all")
    public static List<Term> convertTerms(Object value) {
        if (value instanceof String) {
            String strVal = String.valueOf(value);
            //json字符串
            if (strVal.startsWith("[")) {
                value = JSON.parseArray(strVal);
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
        PrepareSqlFragments fragments = PrepareSqlFragments.of();

        if(term.getOptions().contains("not")){
            fragments.addSql("not");
        }
        fragments.addSql("exists( select 1 from rule_dev_alarm_history _his where", columnFullName)
                 .addSql("= _his.device_id");


        RDBTableMetadata metadata = column
            .getOwner()
            .getSchema()
            .getTable("rule_dev_alarm_history")
            .orElseThrow(() -> new UnsupportedOperationException("unsupported dev-alarm"));

        SqlFragments where = builder.createTermFragments(metadata, terms);

        if (!where.isEmpty()) {
            fragments.addSql("and")
                     .addFragments(where);

        }
        fragments.addSql(")");
        return fragments;
    }

    static AlarmTermBuilder builder = new AlarmTermBuilder();

    static class AlarmTermBuilder extends AbstractTermsFragmentBuilder<TableOrViewMetadata> {

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
                    .map(termFragment -> termFragment.createFragments(column.getFullName("_his"), column, term)))
                .orElse(EmptySqlFragments.INSTANCE);
        }
    }


}
