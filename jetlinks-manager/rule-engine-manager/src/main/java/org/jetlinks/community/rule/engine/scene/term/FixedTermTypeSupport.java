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
package org.jetlinks.community.rule.engine.scene.term;

import lombok.Getter;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @deprecated
 * @see org.jetlinks.community.reactorql.term.FixedTermTypeSupport
 */
@Deprecated
@Getter
public enum FixedTermTypeSupport implements TermTypeSupport {

    eq("等于", "eq"),
    neq("不等于", "neq"),

    gt("大于", "gt", DateTimeType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),
    gte("大于等于", "gte", DateTimeType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),
    lt("小于", "lt", DateTimeType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),
    lte("小于等于", "lte", DateTimeType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),

    btw("在...之间", "btw", DateTimeType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID) {
        @Override
        protected Object convertValue(Object val) {
            return val;
        }
    },
    nbtw("不在...之间", "nbtw", DateTimeType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID) {
        @Override
        protected Object convertValue(Object val) {
            return val;
        }
    },
    in("在...之中", "in", StringType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID, EnumType.ID) {
        @Override
        protected Object convertValue(Object val) {
            return val;
        }
    },
    nin("不在...之中", "nin", StringType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID, EnumType.ID) {
        @Override
        protected Object convertValue(Object val) {
            return val;
        }
    },
    contains_all("全部包含在...之中", "contains_all", ArrayType.ID) {
        @Override
        protected Object convertValue(Object val) {
            return val;
        }
    },
    contains_any("任意包含在...之中", "contains_any", ArrayType.ID) {
        @Override
        protected Object convertValue(Object val) {
            return val;
        }
    },
    not_contains("不包含在...之中", "not_contains", ArrayType.ID) {
        @Override
        protected Object convertValue(Object val) {
            return val;
        }
    },

    like("包含字符", "str_like", StringType.ID),
    nlike("不包含字符", "str_nlike", StringType.ID),

    // gt(math.sub(column,now()),?)
    time_gt_now("距离当前时间大于...秒", "time_gt_now", DateTimeType.ID) {
        @Override
        protected void appendFunction(String column, PrepareSqlFragments fragments) {
            fragments.addSql("gt(math.divi(math.sub(now(),", column, "),1000),");
        }
    },
    time_lt_now("距离当前时间小于...秒", "time_lt_now", DateTimeType.ID){
        @Override
        protected void appendFunction(String column, PrepareSqlFragments fragments) {
            fragments.addSql("lt(math.divi(math.sub(now(),", column, "),1000),");
        }
    };

    private final String text;
    private final Set<String> supportTypes;

    private final String function;

    private FixedTermTypeSupport(String text, String function, String... supportTypes) {
        this.text = text;
        this.function = function;
        this.supportTypes = new HashSet<>(Arrays.asList(supportTypes));
    }

    @Override
    public boolean isSupported(DataType type) {
        return supportTypes.isEmpty() || supportTypes.contains(type.getType());
    }

    protected Object convertValue(Object val) {
        if (val instanceof Collection) {
            //值为数组,则尝试获取第一个值
            if (((Collection<?>) val).size() == 1) {
                return ((Collection<?>) val).iterator().next();
            }
        }
        return val;
    }

    protected void appendFunction(String column, PrepareSqlFragments fragments) {
        fragments.addSql(function + "(", column, ",");
    }

    @Override
    public SqlFragments createSql(String column, Object value) {
        PrepareSqlFragments fragments = PrepareSqlFragments.of();
        appendFunction(column, fragments);
        value = convertValue(value);

        if (value instanceof NativeSql) {
            fragments
                .addSql(((NativeSql) value).getSql())
                .addParameter(((NativeSql) value).getParameters());
        } else {
            fragments.addSql("?")
                     .addParameter(value);
        }
        fragments.addSql(")");
        return fragments;
    }

    @Override
    public String getType() {
        return name();
    }

    @Override
    public String getName() {
        return LocaleUtils.resolveMessage("message.term_type_" + name(), text);
    }
}
