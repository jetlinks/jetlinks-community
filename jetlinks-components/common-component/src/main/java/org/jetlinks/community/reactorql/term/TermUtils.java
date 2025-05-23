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
package org.jetlinks.community.reactorql.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.executor.SqlRequest;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.jetlinks.community.reactorql.function.FunctionSupport;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author zhangji 2025/2/8
 * @since 2.3
 */
public class TermUtils {

    public static List<Term> expandTermToList(List<Term> terms) {
        Map<String, List<Term>> termsMap = expandTerm(terms);
        List<Term> termList = new ArrayList<>();
        for (List<Term> values : termsMap.values()) {
            termList.addAll(values);
        }
        return termList;
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
                if (termList == null) {
                    List<Term> list = new ArrayList<>();
                    list.add(term);
                    container.put(term.getColumn(), list);
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

    public static Term refactorTerm(String tableName,
                                    Term term,
                                    BiFunction<String, String, String> columnRefactor) {
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
                return columnRefactor.apply(tableName, value.getValue().toString());
            }
            //指标
            else if (value.getSource() == TermValue.Source.metric) {
                term.getOptions().add(TermType.OPTIONS_NATIVE_SQL);
                return tableName + "['" + arr[1] + "_metric_" + value.getMetric() + "']";
            }
            //函数, 如: array_len() , device_prop()
            else if (value.getSource() == TermValue.Source.function) {
                SqlRequest request = FunctionSupport
                    .supports
                    .getNow(value.getFunction())
                    .createSql(columnRefactor.apply(tableName, value.getColumn()), value.getArgs())
                    .toRequest();
                return NativeSql.of(request.getSql(), request.getParameters());
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

        if (term.getOptions().contains(TermType.OPTIONS_NATIVE_SQL) && !(val instanceof NativeSql)) {
            val = NativeSql.of(String.valueOf(val));
        }

        term.setColumn(columnRefactor.apply(tableName, term.getColumn()));

        term.setValue(val);

        return term;
    }

}
