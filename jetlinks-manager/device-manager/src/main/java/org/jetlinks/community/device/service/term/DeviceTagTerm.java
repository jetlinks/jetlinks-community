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
 * 根据设备标签查询设备或者与设备关联的数据.
 * <p>
 * <b>
 * 注意: 查询时指定列名是和设备ID关联的列或者实体类属性名.
 * 如: 查询设备列表时则使用id.
 * 此条件仅支持关系型数据库中的查询.
 * </b>
 * <p>
 * 在通用查询接口中可以使用动态查询参数中的<code>term.termType</code>来使用此功能.
 * <a href="https://doc.jetlinks.cn/interface-guide/query-param.html">查看动态查询参数说明</a>
 * <p>
 * 在内部通用条件中,可以使用DSL方式创建条件,例如:
 * <pre>
 *     createQuery()
 *     .where()
 *     .and("id","dev-tag","tag1 = 1 or tag2 = 2")
 *     .fetch()
 * </pre>
 *
 * @author zhouhao
 * @since 1.6
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

        PrepareSqlFragments copy = PrepareSqlFragments.of();
        copy.addSql(fragments.getSql());
        int len = 0;
        fragments.addSql("and (");
        for (Object tag : tags) {
            String key;
            String value;
            if (tag instanceof Map) {
                @SuppressWarnings("all")
                Map<Object, Object> map = ((Map) tag);
                if (map.get("type") != null) {
                    and = Term.Type.and.name().equals(map.get("type"));
                }
                //key or column
                key = String.valueOf(map.getOrDefault("key", map.get("column")));
                value = String.valueOf(map.get("value"));
            } else if (tag instanceof Term) {
                    Term.Type type = ((Term) tag).getType();
                    if (type != null) {
                        and = Term.Type.and.equals(type);
                    }
                key = ((Term) tag).getColumn();
                value = String.valueOf(((Term) tag).getValue());
            } else {
                throw new IllegalArgumentException("illegal tag value format");
            }
            if (len++ > 0) {
                // 组合多个exist语句：and (exists(...) and exist(...))
                fragments.addSql("))");
                fragments.addSql(and ? "and" : "or");
                fragments.add(copy);
                fragments.addSql("and");
                fragments.addSql("(d.key = ? and d.value like ?)")
                         .addParameter(key, value);
            } else {
                fragments.addSql("(d.key = ? and d.value like ?)")
                         .addParameter(key, value);
            }
        }
        if (tags.isEmpty()) {
            fragments.addSql("1=2");
        }
        fragments.addSql(")");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {


        PrepareSqlFragments fragments = PrepareSqlFragments.of();
        fragments.addSql("(");
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
        fragments.addSql(")");

        return fragments;
    }

    static WhereBuilder builder = new WhereBuilder();

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
