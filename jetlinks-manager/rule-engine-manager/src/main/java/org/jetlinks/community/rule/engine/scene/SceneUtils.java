package org.jetlinks.community.rule.engine.scene;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.scene.value.TermValue;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;

public class SceneUtils {

    public static String createColumnAlias(String prefix,String column, boolean wrapColumn) {
        if (!column.contains(".")) {
            return wrapColumn ? wrapColumnName(column) : column;
        }
        String[] arr = column.split("[.]");
        String alias;
        //prefix.temp.current
        if (prefix.equals(arr[0])) {
            String property = arr[1];
            alias = property + "_" + arr[arr.length - 1];
        } else {
            if (arr.length > 1) {
                alias = String.join("_", Arrays.copyOfRange(arr, 1, arr.length));
            } else {
                alias = column.replace(".", "_");
            }
        }
        return wrapColumn ? wrapColumnName(alias) : alias;
    }

    public static String wrapColumnName(String column) {
        if (column.startsWith("\"") && column.endsWith("\"")) {
            return column;
        }
        return "\"" + (column.replace("\"", "\\\"")) + "\"";
    }

    /**
     * 根据条件和可选的条件列解析出将要输出的变量信息
     *
     * @param terms   条件
     * @param columns 列信息
     * @return 变量信息
     */
    public static List<Variable> parseVariable(List<Term> terms,
                                               List<TermColumn> columns) {
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
        String variableName = column.getName(); //prefixName == null ? column.getName() : prefixName + "/" + column.getName();

        if (CollectionUtils.isEmpty(column.getChildren())) {
            Term term = termSupplier.apply(column.getColumn());
            variables.add(Variable.of(column.getVariable("_"), variableName)
                                  .with(column)
            );
            if (term != null) {
                List<TermValue> termValues = TermValue.of(term);
                String property = column.getPropertyOrNull();
                for (TermValue termValue : termValues) {
                    PropertyMetric metric = column.getMetricOrNull(termValue.getMetric());
                    if (property != null && metric != null && termValue.getSource() == TermValue.Source.metric) {
                        // temp_metric
                        variables.add(Variable.of(
                                                  property + "_metric_" + termValue.getMetric(),
                                                  (prefixName == null ? column.getName() : prefixName) + "_指标_" + metric.getName())
                                              .withTermType(column.getTermTypes())
                                              .withColumn(column.getColumn())
                                              .withMetadata(column.isMetadata())
                        );
                    }
                }
            }

        } else {
            Variable variable = Variable.of(column.getColumn(), column.getName());
            List<Variable> children = new ArrayList<>();
            variable.setChildren(children);
            variable.with(column);

            variables.add(variable);
            for (TermColumn child : column.getChildren()) {
                children.addAll(columnToVariable(column.getName(), child, termSupplier));
            }
        }
        return variables;
    }
}
