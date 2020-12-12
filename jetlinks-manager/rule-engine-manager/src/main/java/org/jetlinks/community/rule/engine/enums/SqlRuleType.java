package org.jetlinks.community.rule.engine.enums;

import com.alibaba.fastjson.annotation.JSONType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.hswebframework.web.dict.EnumDict;
import org.jetlinks.community.rule.engine.ql.SqlRule;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.util.Assert;

@Getter
@AllArgsConstructor
@JSONType(deserializer = EnumDict.EnumDictJSONDeserializer.class)
public enum SqlRuleType implements EnumDict<String> {

    timer("定时") {
        @Override
        public void validate(SqlRule rule) {
            Assert.notNull(rule.getCron(), "cron表达式不能为空");
            try {
                new CronSequenceGenerator(rule.getCron());
            } catch (Exception e) {
                throw new IllegalArgumentException("cron表达式格式错误", e);
            }
        }
    },
    realTime("实时") {
        @Override
        public void validate(SqlRule rule) {
            Assert.notNull(rule.getSql(), "sql不能为空");
            try {
                PlainSelect select = ((PlainSelect) ((Select) CCJSqlParserUtil.parse(rule.getSql())).getSelectBody());
                if (select.getGroupBy() != null && select.getGroupBy().getGroupByExpressions() != null) {
                    for (Expression groupByExpression : select.getGroupBy().getGroupByExpressions()) {
                        if (groupByExpression instanceof Function) {
                            String name = ((Function) groupByExpression).getName();
                            if ("interval".equalsIgnoreCase(name) || "_window".equalsIgnoreCase(name)) {
                                return;
                            }
                        }
                    }
                    throw new IllegalArgumentException("实时数据处理必须指定分组函数interval或者_window");
                }
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalArgumentException("sql格式错误", e);
            }
        }
    };

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    public abstract void validate(SqlRule rule);
}
