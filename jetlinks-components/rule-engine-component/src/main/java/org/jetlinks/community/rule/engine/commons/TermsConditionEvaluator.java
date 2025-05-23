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
package org.jetlinks.community.rule.engine.commons;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.exception.I18nSupportException;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.community.utils.ReactorUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataHelper;
import org.jetlinks.rule.engine.api.model.Condition;
import org.jetlinks.rule.engine.condition.ConditionEvaluatorStrategy;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 使用条件对象或者条件表达式来作为规则执行判断条件
 *
 * <pre>{@code
 *
 *  {
 *      "type":"terms",
 *      "configuration":{
 *          "terms":[
 *              {"column":"data.val","value":"10"}
 *         ],
 *         //terms存在时,where无效
 *         "where":"data.val eq 10"
 *      }
 *
 *  }
 *
 *  }</pre>
 * <p>
 * 建议使用{@link TermsConditionEvaluator#createCondition(List)}来创建用户输入的条件
 *
 * @author zhouhao
 * @see TermsConditionEvaluator#createCondition(List)
 * @since 2.0
 */
public class TermsConditionEvaluator implements ConditionEvaluatorStrategy {
    public static final String TYPE = "terms";

    public static Condition createCondition(List<Term> terms) {
        //校验条件是否合法
        validateUnsafeTerm(terms);

        Condition condition = new Condition();
        condition.setType(TYPE);
        condition.setConfiguration(Collections.singletonMap("terms", terms));
        return condition;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public boolean evaluate(Condition condition, RuleData context) {
        return false;
    }

    public static void validateUnsafeTerm(List<Term> term) {
        if (CollectionUtils.isNotEmpty(term)) {
            term.forEach(TermsConditionEvaluator::validateUnsafeTerm);
        }
    }

    public static void validateUnsafeTerm(Term term) {
        Object val = term.getValue();
        //如果值是Map,并且包含sql字段,说明是有用户输入了sql参数.
        //程序构造应当使用NativeSql
        if (val instanceof Map) {
            @SuppressWarnings("all")
            JSONObject map = new JSONObject((Map) val);
            if (map.containsKey("sql")) {
                throw new I18nSupportException("error.illegal_term_value");
            }
        }
        validateUnsafeTerm(term.getTerms());
    }

    private Term convertTermValue(Term term) {
        Object val = term.getValue();
        if (val instanceof Map) {
            @SuppressWarnings("all")
            JSONObject map = new JSONObject((Map) val);
            //nativeSql,这里存在缺陷? 用户在特殊情况下可以传入任意sql(不使用 createCondition 方法创建的条件)
            //但是此sql使用reactorQL执行,不会直接执行到数据库,所以不会有sql注入风险.
            if (map.containsKey("sql")) {
                String sql = map.getString("sql");
                Object[] params = map.containsKey("parameters") ? map
                    .getJSONArray("parameters")
                    .toArray() : new Object[0];
                term.setValue(NativeSql.of(sql, params));
            }
        }
        if (CollectionUtils.isNotEmpty(term.getTerms())) {
            for (Term termTerm : term.getTerms()) {
                convertTermValue(termTerm);
            }
        }
        return term;
    }

    @Override
    public Function<RuleData, Mono<Boolean>> prepare(Condition condition) {
        List<Term> terms = condition
            .getConfig("terms")
            .map(val -> CastUtils
                .castArray(val)
                .stream()
                .map(obj -> convertTermValue(FastBeanCopier.copy(obj, new Term())))
                .collect(Collectors.toList()))
            .orElseGet(() -> condition
                .getConfig("where")
                .map(String::valueOf)
                .map(TermExpressionParser::parse)
                .orElseThrow(() -> new IllegalArgumentException("terms can not be null"))
            );

        return ReactorUtils.createFilter(terms,
                                         RuleDataHelper::toContextMap,
                                         VariableSource::resolveStatic);
    }
}
