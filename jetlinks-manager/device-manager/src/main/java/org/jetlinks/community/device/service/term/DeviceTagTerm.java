package org.jetlinks.community.device.service.term;

import com.alibaba.fastjson.JSON;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.AbstractTermsFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.TermFragmentBuilder;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * where("id$dev-tag$location","重庆")
 */
@Component
public class DeviceTagTerm extends AbstractTermFragmentBuilder {

    public static final String termType = "dev-tag";

    public DeviceTagTerm() {
        super(termType, "根据设备标签查询设备");
    }

    private void acceptTerm(boolean and, RDBColumnMetadata column, PrepareSqlFragments fragments, String terms) {
        //json
        if (terms.startsWith("[")) {
            acceptTerm(and, fragments, JSON.parseArray(terms, Map.class));
        } else if (terms.startsWith("{")) {
            acceptTerm(and, fragments, JSON.parseObject(terms));
        } else if (terms.contains(":") && !terms.contains(" ")) {
            List<Map<String, String>> tags = Stream
                .of(terms.split(","))
                .map(str -> str.split(":"))
                .map(str -> {
                    Map<String, String> tag = new HashMap<>();
                    tag.put("key", str[0]);
                    tag.put("value", str.length > 1 ? str[1] : null);
                    return tag;
                })
                .collect(Collectors.toList());
            acceptTerm(and, fragments, tags);
        } else {
            //SQL表达式
            List<Term> tagKeys = TermExpressionParser.parse(terms);
            SqlFragments expr = builder.createTermFragments(column, tagKeys);
            if (expr.isNotEmpty()) {
                fragments.addSql("and (").addFragments(expr).addSql(")");
            }
        }

    }

    private void acceptTerm(boolean and, PrepareSqlFragments fragments, Map<?, ?> terms) {
        acceptTerm(and, fragments, terms.entrySet().stream().map(e -> {
            Map<String, String> tag = new HashMap<>();
            tag.put("key", String.valueOf(e.getKey()));
            tag.put("value", String.valueOf(e.getValue()));
            return tag;
        }).collect(Collectors.toList()));
    }

    private void acceptTerm(boolean and, PrepareSqlFragments fragments, Collection<?> tags) {

        int len = 0;
        fragments.addSql("and (");
        for (Object tag : tags) {
            if (len++ > 0) {
                fragments.addSql(and ? "and" : "or");
            }
            String key;
            String value;
            if (tag instanceof Map) {
                @SuppressWarnings("all")
                Map<Object, Object> map = ((Map) tag);
                //key or column
                key = String.valueOf(map.getOrDefault("key", map.get("column")));
                value = String.valueOf(map.get("value"));
            } else if (tag instanceof Term) {
                key = ((Term) tag).getColumn();
                value = String.valueOf(((Term) tag).getValue());
            } else {
                throw new IllegalArgumentException("illegal tag value format");
            }
            fragments.addSql("(d.key = ? and d.value like ?)")
                     .addParameter(key, value);
        }
        if (tags.isEmpty()) {
            fragments.addSql("1=2");
        }
        fragments.addSql(")");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {


        PrepareSqlFragments fragments = PrepareSqlFragments.of();
        fragments.addSql("exists(select 1 from ",getTableName("dev_device_tags",column)," d where d.device_id =", columnFullName);
        Object value = term.getValue();
        boolean and = term.getOptions().contains("and");
        if (value instanceof Map) {
            acceptTerm(and, fragments, (Map<?, ?>) value);
        } else if (value instanceof Collection) {
            acceptTerm(and, fragments, (Collection<?>) value);
        } else {
            acceptTerm(and, column, fragments, String.valueOf(value));
        }

        fragments.addSql(")");

        return fragments;
    }

    static DeviceTagTerm.WhereBuilder builder = new DeviceTagTerm.WhereBuilder();

    static class WhereBuilder extends AbstractTermsFragmentBuilder<RDBColumnMetadata> {


        @Override
        protected SqlFragments createTermFragments(RDBColumnMetadata parameter, Term term) {

            PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
            sqlFragments.addSql("(d.key = ?")
                        .addParameter(term.getColumn())
                        .addSql("and");
            if (parameter == null) {
                sqlFragments.addSql("d.value = ?").addParameter(term.getValue());
            } else {
                sqlFragments.addFragments(parameter
                                              .findFeatureNow(TermFragmentBuilder.createFeatureId(term.getTermType()))
                                              .createFragments("d.value", parameter, term)
                );
            }
            sqlFragments
                .addSql(")");
            return sqlFragments;
        }

        @Override
        protected SqlFragments createTermFragments(RDBColumnMetadata parameter, List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }
    }
}