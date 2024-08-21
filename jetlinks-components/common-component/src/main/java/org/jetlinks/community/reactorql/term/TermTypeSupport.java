package org.jetlinks.community.reactorql.term;

import lombok.SneakyThrows;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.jetlinks.community.utils.ReactorUtils;
import org.jetlinks.core.metadata.DataType;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

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
        return String.format("%s%s(%s)", property, getName(), expect);
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
