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
            acceptTerm(and, fragments, (List) JSON.parseArray(terms, Map.class));
        } else if (terms.startsWith("{")) {
            acceptTerm(and, fragments, JSON.parseObject(terms));
        } else if (terms.contains(":") && !terms.contains(" ")) {
            List<Map<String, String>> tags = Stream
                .of(terms.split("[,]"))
                .map(str -> str.split("[:]"))
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
            fragments.addSql("and (").addFragments(builder.createTermFragments(column, tagKeys)).addSql(")");
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

    private void acceptTerm(boolean and, PrepareSqlFragments fragments, List<Map<String, String>> tags) {

        int len = 0;
        fragments.addSql("and (");
        for (Map<String, String> tag : tags) {
            if (len++ > 0) {
                fragments.addSql(and ? "and" : "or");
            }
            fragments.addSql("(d.key = ? and d.value like ?)").addParameter(tag.get("key"), tag.get("value"));
        }
        if (tags.isEmpty()) {
            fragments.addSql("1=2");
        }
        fragments.addSql(")");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {


        PrepareSqlFragments fragments = PrepareSqlFragments.of();

        fragments.addSql("exists(select 1 from dev_device_tags d where d.device_id =", columnFullName);
        Object value = term.getValue();
        boolean and = term.getOptions().contains("and");
        if (value instanceof Map) {
            acceptTerm(and, fragments, (Map<?, ?>) value);
        } else if (value instanceof List) {
            acceptTerm(and, fragments, (List<Map<String, String>>) value);
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
                        .addSql("and")
                        .addFragments(parameter
                                          .findFeatureNow(TermFragmentBuilder.createFeatureId(term.getTermType()))
                                          .createFragments("d.value", parameter, term)
                        ).addSql(")");
            return sqlFragments;
        }

        @Override
        protected SqlFragments createTermFragments(RDBColumnMetadata parameter, List<Term> terms) {
            return super.createTermFragments(parameter, terms);
        }
    }
}
