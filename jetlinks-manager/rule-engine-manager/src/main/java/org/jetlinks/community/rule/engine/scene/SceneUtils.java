package org.jetlinks.community.rule.engine.scene;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.scene.value.TermValue;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SceneUtils {


    /**
     * 根据条件和可选的条件列解析出将要输出的变量信息
     *
     * @param terms   条件
     * @param columns 列信息
     * @return 变量信息
     */
    public static List<Variable> parseVariable(List<Term> terms,
                                               List<TermColumn> columns) {
//        if (CollectionUtils.isEmpty(terms)) {
//            return Collections.emptyList();
//        }
        //平铺条件
        Map<String, Term> termCache = expandTerm(terms);

        //解析变量
        List<Variable> variables = new ArrayList<>(termCache.size());
        for (TermColumn column : columns) {
            variables.addAll(columnToVariable(null, column, termCache::get));
        }

        return variables;
    }

    public static Map<String, Term> expandTerm(List<Term> terms) {
        Map<String, Term> termCache = new LinkedHashMap<>();
        expandTerm(terms, termCache);
        return termCache;
    }

    private static void expandTerm(List<Term> terms, Map<String, Term> container) {
        if (terms == null) {
            return;
        }
        for (Term term : terms) {
            if (StringUtils.hasText(term.getColumn())) {
                container.put(term.getColumn(), term);
            }
            if (term.getTerms() != null) {
                expandTerm(term.getTerms(), container);
            }
        }
    }

    private static List<Variable> columnToVariable(String prefixName,
                                                   TermColumn column,
                                                   Function<String, Term> termSupplier) {
        List<Variable> variables = new ArrayList<>(1);
        String variableName = prefixName == null ? column.getName() : prefixName + "_" + column.getName();

        if (CollectionUtils.isEmpty(column.getChildren())) {
            Term term = termSupplier.apply(column.getColumn());
            if (term != null) {
                //有条件的数据会有别名 以_分隔
                variables.add(Variable
                    .of(column.getVariable("_"), variableName)
                    .withType(column.getDataType()));
                List<TermValue> termValues = TermValue.of(term);
                String property = column.getPropertyOrNull();
                for (TermValue termValue : termValues) {
                    PropertyMetric metric = column.getMetricOrNull(termValue.getMetric());
                    if (property != null && metric != null && termValue.getSource() == TermValue.Source.metric) {
                        // temp_metric
                        variables.add(Variable.of(
                            property + "_metric_" + termValue.getMetric(),
                            (prefixName == null ? column.getName() : prefixName) + "_指标_" + metric.getName()));
                    }
                }
            } else {
                //没有条件,没有别名
                variables.add(Variable.of(column.getVariable("."), variableName));
            }

        } else {
            for (TermColumn child : column.getChildren()) {
                variables.addAll(columnToVariable(variableName, child, termSupplier));
            }
        }
        return variables;
    }
}
