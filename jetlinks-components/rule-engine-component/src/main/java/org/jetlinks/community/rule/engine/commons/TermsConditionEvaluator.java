package org.jetlinks.community.rule.engine.commons;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.TermExpressionParser;
import org.hswebframework.web.bean.FastBeanCopier;
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
 *
 * @author zhouhao
 * @since 2.0
 */
public class TermsConditionEvaluator implements ConditionEvaluatorStrategy {
    public static final String TYPE = "terms";

    public static Condition createCondition(List<Term> terms) {
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

    @Override
    public Function<RuleData, Mono<Boolean>> prepare(Condition condition) {
        List<Term> terms = condition
            .getConfig("terms")
            .map(val -> CastUtils
                .castArray(val)
                .stream()
                .map(obj -> FastBeanCopier.copy(obj, new Term()))
                .collect(Collectors.toList()))
            .orElseGet(() -> condition
                .getConfig("where")
                .map(String::valueOf)
                .map(TermExpressionParser::parse)
                .orElseThrow(() -> new IllegalArgumentException("terms can not be null"))
            );

        return ReactorUtils.createFilter(terms,
                                         RuleDataHelper::toContextMap,
                                         (arg, map) -> VariableSource.of(arg).resolveStatic(map));
    }
}
