package org.jetlinks.community.rule.engine.scene;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.reactorql.term.TermUtils;
import org.jetlinks.community.reactorql.term.TermValue;
import org.jetlinks.community.relation.utils.VariableSource;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorProviders;
import org.jetlinks.community.rule.engine.executor.device.DeviceSelectorSpec;
import org.jetlinks.community.rule.engine.scene.term.TermColumn;
import org.jetlinks.community.rule.engine.web.response.SelectorInfo;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SceneUtils {

    //关键字，用于重构别名时条件判断
    private final static List<String> keys = Lists.newArrayList("current", "recent", "last", "lastTime");

    public static String createColumnAlias(String prefix, String column, boolean wrapColumn) {
        if (!column.contains(".")) {
            return wrapColumn ? wrapColumnName(column) : column;
        }
        String[] arr = column.split("[.]");
        String alias;
        //prefix.temp.current -> temp_current
        if (StringUtils.hasText(prefix) && prefix.equals(arr[0])) {
            if (arr.length == 2) {
                alias = arr[1];
            } else {
                alias = String.join("_", Arrays.copyOfRange(arr, 1, arr.length));
            }
        } else {
            if (!isContainThis(arr)) {
                //someObj.obj.column -> obj_column
                if (arr.length > 1 && !isContainKey(arr, keys)) {
                    alias = String.join("_", Arrays.copyOfRange(arr, 1, arr.length));
                } else {
                    //someObj.obj.column.recent -> someObj_obj_column_recent
                    alias = column.replace(".", "_");
                }
            } else {
                //直接将 xx.this.recent 解析中间的this忽略解析为 xx_recent， latest同理
                String[] array = Arrays.stream(arr)
                    .filter(str -> !"this".equals(str))
                    .toArray(String[]::new);
                alias = String.join("_", array);
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
        Map<String, List<Term>> termCache = TermUtils.expandTerm(terms);

        //解析变量
        List<Variable> variables = new ArrayList<>(termCache.size());
        for (TermColumn column : columns) {
            variables.addAll(columnToVariable(null, column, termCache::get));
        }

        return variables;
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

    public static Flux<SceneTriggerProvider<SceneTriggerProvider.TriggerConfig>> getSupportTriggers() {
        return Flux
            .fromIterable(SceneProviders.triggerProviders())
            .filterWhen(SceneTriggerProvider::isSupported);
    }

    public static Flux<SceneActionProvider<?>> getSupportActions() {
        return Flux
            .fromIterable(SceneProviders.actionProviders())
            .filterWhen(SceneActionProvider::isSupported);
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
        return TermUtils.refactorTerm(tableName, term, SceneUtils::refactorColumn);
    }

    private static String refactorColumn(String tableName, String column) {
        String[] arr = column.split("[.]");
        // fixme 重构 条件列解析逻辑
        // properties.xxx.last的场景
        if (arr.length > 3 && arr[0].equals("properties")) {
            String refactorColumn =  tableName + "['" + createColumnAlias("properties", column, false);
            // 上一次上报时间不需要计算
            if (!DeviceOperation.PropertyValueType.lastTime.name().equals(arr[arr.length -1])) {
                refactorColumn += "." + String.join(".", Arrays.copyOfRange(arr, 2, arr.length - 1));
            }
            refactorColumn += "']";
            return refactorColumn;
        } else if (!isDirectTerm(arr[0])) {
            return tableName + "['" + createColumnAlias(arr[0], column, false) + "']";
        } else {
            // scene.obj1.xx.val1.current => t['scene.obj1_current.val1']
            if (isSceneTerm(column)) {
                String refactorColumn = tableName + "['" + arr[0] + "." + createColumnAlias(arr[0], column, false);
                if (arr.length > 3) {
                    refactorColumn = refactorColumn +
                        "." + String.join(".", Arrays.copyOfRange(arr, 2, arr.length - 1));
                }
                refactorColumn += "']";
                return refactorColumn;
            } else {
                return tableName + "['" + column + "']";
            }
        }
    }

    @SuppressWarnings("all")
    public static void refactorUpperKey(Object source) {
        // 将变量格式改为与查询的别名一致
        if (source instanceof VariableSource) {
            VariableSource variableSource = (VariableSource) source;
            if (VariableSource.Source.upper.equals(variableSource.getSource())) {
                variableSource.setUpperKey(transferSceneUpperKey(variableSource.getUpperKey()));
            }
        }
        if (source instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) source;
            VariableSource variableSource = VariableSource.of(source);
            // 将变量格式改为与查询的别名一致
            if (VariableSource.Source.upper.equals(variableSource.getSource())) {
                map.put("upperKey", transferSceneUpperKey(variableSource.getUpperKey()));
            }
        }
    }

    public static String transferSceneUpperKey(String upperKey) {
        // scene.xx.current -> scene.scene_xx_current
        if (upperKey.startsWith("scene.")) {
            String alias = SceneUtils.createColumnAlias("scene", upperKey, false);
            return "scene." + alias;
        }
        return upperKey;
    }

    private static boolean isContainThis(String[] arr) {
        return Arrays.stream(arr)
            .collect(Collectors.toList())
            .contains("this")
            ;
    }

    private static boolean isContainKey(String[] arr, List<String> keys) {
        return !Collections.disjoint(Arrays.stream(arr)
            .collect(Collectors.toList()), keys)

            ;
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
