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

import lombok.SneakyThrows;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.community.utils.ReactorUtils;
import org.jetlinks.core.metadata.DataType;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * 查询条件类型支持
 *
 * @author zhouhao
 * @see org.jetlinks.community.utils.ReactorUtils#createFilter(List)
 * @since 2.0
 */
public interface TermTypeSupport {

    /**
     * @return 条件标识
     */
    String getType();

    /**
     * @return 条件名称
     */
    String getName();

    /**
     * 判断是否支持特定的数据类型
     *
     * @param type 数据类型
     * @return 是否支持
     */
    boolean isSupported(DataType type);

    /**
     * 创建SQL片段
     *
     * @param column 列名
     * @param value  值
     * @param term   条件
     * @return SQL片段
     */
    SqlFragments createSql(String column, Object value, Term term);

    /**
     * 重构条件
     *
     * @param tableName 表名
     * @param term      条件
     * @param refactor   重构函数
     * @return 重构后的条件
     */
    default Term refactorTerm(String tableName, Term term, BiFunction<String,Term,Term> refactor){
        return refactor.apply(tableName,term);
    }

    /**
     * 判断是否已经过时,过时的条件应当不可选择.
     *
     * @return 是否已经过时
     */
    default boolean isDeprecated() {
        return false;
    }

    /**
     * 转为条件类型
     *
     * @return 条件类型
     */
    default TermType type() {
        return TermType.of(getType(), getName());
    }


    default String createDesc(String property, Object expect, Object actual) {

        return LocaleUtils.resolveMessage(
            "message.term_" + getType() + "_desc",
            String.format("%s%s(%s)", property, getName(), expect),
            property,
            getName(),
            expect,
            actual
        );
    }

    default String createActualDesc(String property, Object actual) {
        return property + " = " + actual;
    }

    /**
     * 阻塞方式判断能否满足期望值
     *
     * @param expect 期望值
     * @param actual 实际值
     * @return 是否满足
     */
    @SneakyThrows
    default boolean matchBlocking(Object expect, Object actual) {
        return this
            .match(expect, actual)
            .toFuture()
            .get(1, TimeUnit.SECONDS);
    }

    /**
     * 判断能否满足期望值
     *
     * @param expect 期望值
     * @param actual 实际值
     * @return 是否满足
     */
    default Mono<Boolean> match(Object expect, Object actual) {
        Term term = new Term();
        term.setTermType(getType());
        term.setColumn("_mock");
        term.setValue(expect);

        return ReactorUtils
            .createFilter(Collections.singletonList(term))
            .apply(Collections.singletonMap("_mock", actual));
    }
}
