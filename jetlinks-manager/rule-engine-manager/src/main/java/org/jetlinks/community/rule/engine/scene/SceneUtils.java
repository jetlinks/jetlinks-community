package org.jetlinks.community.rule.engine.scene;

import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.reactorql.term.TermType;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.scene.value.TermValue;
import org.jetlinks.community.rule.engine.web.response.SelectorInfo;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SceneUtils {


    public static String createColumnAlias(String prefix, String column, boolean wrapColumn) {
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

    public static String appendColumn(String... columns) {
        StringJoiner joiner = new StringJoiner(".");
        for (String column : columns) {
            if (StringUtils.hasText(column)) {
                joiner.add(column);
            }
        }
        return joiner.toString();
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
        Map<String, List<Term>> termCache = expandTerm(terms);

        //解析变量
        List<Variable> variables = new ArrayList<>(termCache.size());
        for (TermColumn column : columns) {
            variables.addAll(columnToVariable(null, column, termCache::get));
        }

        return variables;
    }

    public static Map<String, List<Term>> expandTerm(List<Term> terms) {
        Map<String, List<Term>> termCache = new LinkedHashMap<>();
        expandTerm(terms, termCache);
        return termCache;
    }

    private static void expandTerm(List<Term> terms, Map<String, List<Term>> container) {
        if (terms == null) {
            return;
        }
        for (Term term : terms) {
            if (StringUtils.hasText(term.getColumn())) {
                List<Term> termList = container.get(term.getColumn());
                if (termList == null){
                    List<Term> list = new ArrayList<>();
                    list.add(term);
                    container.put(term.getColumn(),list);
                } else {
                    termList.add(term);
                    container.put(term.getColumn(), termList);
                }
            }
            if (term.getTerms() != null) {
                expandTerm(term.getTerms(), container);
            }
        }
    }

    private static List<Variable> columnToVariable(String prefixName,
                                                   TermColumn column,
                                                   Function<String, List<Term>> termSupplier) {
        List<Variable> variables = new ArrayList<>(1);
        String variableName = column.getName(); //prefixName == null ? column.getName() : prefixName + "/" + column.getName();

        if (CollectionUtils.isEmpty(column.getChildren())) {
            List<Term> termList = termSupplier.apply(column.getColumn());
            variables.add(Variable.of(column.getVariable("_"), variableName)
                                  .with(column)
            );
            if (termList != null && !termList.isEmpty()) {
                for (Term term : termList) {
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
                                                  .withCode(column.getCode())
                                                  .withFullNameCode(column.getFullNameCode().copy())
                                                  .withMetadata(column.isMetadata())
                            );
                        }
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

    public static Flux<String> getSupportTriggers() {
        return Flux
            .fromIterable(SceneProviders.triggerProviders())
            .map(SceneTriggerProvider::getProvider);
    }

    public static Flux<String> getSupportActions() {
        return Flux
            .fromIterable(SceneProviders.actionProviders())
            .map(SceneActionProvider::getProvider);
    }

    public static Flux<TermColumn> parseTermColumns(SceneRule ruleMono) {
        Trigger trigger = ruleMono.getTrigger();
        if (trigger != null) {
            return trigger.parseTermColumns();
        }
        return Flux.empty();
    }

    public static Flux<Variable> parseVariables(Mono<SceneRule> ruleMono, Integer branch, Integer branchGroup, Integer action) {
        Mono<SceneRule> cache = ruleMono.cache();
        return Mono
            .zip(
                cache.flatMapMany(SceneUtils::parseTermColumns).collectList(),
                cache,
                (columns, rule) -> rule
                    .createVariables(columns,
                                     branch,
                                     branchGroup,
                                     action))
            .flatMapMany(Function.identity());
    }

    public static Flux<SelectorInfo> getDeviceSelectors() {
        return Flux
            .fromIterable(DeviceSelectorProviders.allProvider())
            //场景联动的设备动作必须选择一个产品,不再列出产品
            .filter(provider -> !"product".equals(provider.getProvider()))
            .map(SelectorInfo::of);
    }

    public static Term refactorTerm(String tableName, Term term) {
        if (term.getColumn() == null) {
            return term;
        }
        String[] arr = term.getColumn().split("[.]");

        List<TermValue> values = TermValue.of(term);
        if (values.isEmpty()) {
            return term;
        }

        Function<TermValue, Object> parser = value -> {
            //上游变量
            if (value.getSource() == TermValue.Source.variable
                || value.getSource() == TermValue.Source.upper) {
                term.getOptions().add(TermType.OPTIONS_NATIVE_SQL);
                return tableName + "['" + value.getValue() + "']";
            }
            //指标
            else if (value.getSource() == TermValue.Source.metric) {
                term.getOptions().add(TermType.OPTIONS_NATIVE_SQL);
                return tableName + "['" + arr[1] + "_metric_" + value.getMetric() + "']";
            }
            //手动设置值
            else {
                return value.getValue();
            }
        };
        Object val;
        if (values.size() == 1) {
            val = parser.apply(values.get(0));
        } else {
            val = values
                .stream()
                .map(parser)
                .collect(Collectors.toList());
        }

        String column;
        // properties.xxx.last的场景
        if (arr.length > 3 && arr[0].equals("properties")) {
            column = tableName + "['" + createColumnAlias("properties", term.getColumn(), false)
                + "." + String.join(".", Arrays.copyOfRange(arr, 2, arr.length - 1)) + "']";
        } else if (!isDirectTerm(arr[0])) {
            column = tableName + "['" + createColumnAlias("properties", term.getColumn(), false) + "']";
        } else {
            column = term.getColumn();
        }

        if (term.getOptions().contains(TermType.OPTIONS_NATIVE_SQL) && !(val instanceof NativeSql)) {
            val = NativeSql.of(String.valueOf(val));
        }

        term.setColumn(column);

        term.setValue(val);

        return term;
    }


    private static boolean isDirectTerm(String column) {
        //直接term,构建Condition输出条件时使用
        return isBranchTerm(column) || isSceneTerm(column);
    }

    private static boolean isBranchTerm(String column) {
        return column.startsWith("branch_") &&
            column.contains("_group_")
            && column.contains("_action_");
    }

    private static boolean isSceneTerm(String column) {
        return column.startsWith("scene");
    }

}
