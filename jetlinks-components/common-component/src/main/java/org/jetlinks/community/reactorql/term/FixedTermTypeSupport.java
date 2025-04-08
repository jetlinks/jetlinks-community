package org.jetlinks.community.reactorql.term;

import com.google.common.collect.Sets;
import lombok.Getter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.NativeSql;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.web.i18n.LocaleUtils;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.community.utils.ConverterUtils;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum FixedTermTypeSupport implements TermTypeSupport {

    eq("等于", "eq") {
        @Override
        public boolean isSupported(DataType type) {
            return !type.getType().equals(ArrayType.ID) && super.isSupported(type);
        }
    },
    neq("不等于", "neq") {
        @Override
        public boolean isSupported(DataType type) {
            return !type.getType().equals(ArrayType.ID) && super.isSupported(type);
        }

    },
    notnull("不为空", "notnull", false) {
        @Override
        protected String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s%s", property, getName());
        }
    },
    isnull("为空", "isnull", false) {
        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s%s", property, getName());
        }
    },

    gt("大于", "gt", DateTimeType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),
    gte("大于等于", "gte", DateTimeType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),
    lt("小于", "lt", DateTimeType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),
    lte("小于等于", "lte", DateTimeType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID),

    btw("在...之间", "btw", DateTimeType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return val;
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s在%s之间", property, arrayToSpec(expect));
        }
    },
    nbtw("不在...之间", "nbtw", DateTimeType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return val;
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s不在%s之间", property, createValueDesc(expect));
        }
    },
    in("在...之中", "in", StringType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID, EnumType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return val;
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s在%s之中", property, createValueDesc(expect));
        }
    },
    nin("不在...之中", "nin", StringType.ID, ShortType.ID, IntType.ID, LongType.ID, FloatType.ID, DoubleType.ID, EnumType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return val;
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s不在%s之中", property, createValueDesc(expect));
        }
    },
    contains_all("全部包含在...之中", "contains_all", ArrayType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return ConverterUtils.convertToList(val);
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s全部包含在%s之中", property, createValueDesc(expect));
        }
    },
    contains_any("任意包含在...之中", "contains_any", ArrayType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return ConverterUtils.convertToList(val);
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s任意包含在%s之中", property, createValueDesc(expect));
        }
    },
    not_contains("不包含在...之中", "not_contains", ArrayType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return ConverterUtils.convertToList(val);
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s不包含在%s之中", property, createValueDesc(expect));
        }
    },

    like("包含字符", "str_like", StringType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            if (val instanceof NativeSql) {
                NativeSql sql = ((NativeSql) val);
                return NativeSql.of("concat('%'," + sql.getSql() + ",'%')");
            }
            val = super.convertValue(val, term);
            if (val instanceof String && !((String) val).contains("%")) {
                val = "%" + val + "%";
            }
            return val;
        }
    },
    nlike("不包含字符", "str_nlike", StringType.ID) {
        @Override
        protected Object convertValue(Object val, Term term) {
            return like.convertValue(val, term);
        }
    },

    // gt(math.sub(column,now()),?)
    time_gt_now("距离当前时间大于...秒", "time_gt_now", DateTimeType.ID) {
        @Override
        protected void appendFunction(String column, PrepareSqlFragments fragments) {
            fragments.addSql("gt(math.divi(math.sub(now(),", column, "),1000),");
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s距离当前时间大于%s秒", property, expect);
        }
    },
    time_lt_now("距离当前时间小于...秒", "time_lt_now", DateTimeType.ID) {
        @Override
        protected void appendFunction(String column, PrepareSqlFragments fragments) {
            fragments.addSql("lt(math.divi(math.sub(now(),", column, "),1000),");
        }

        @Override
        protected String createValueDesc(Object expect) {
            return arrayToSpec(expect);
        }

        @Override
        public String createDefaultDesc(String property, Object expect, Object actual) {
            return String.format("%s距离当前时间小于%s秒", property, expect);
        }
    };

    private final String text;
    private final boolean needValue;

    private final Set<String> supportTypes;

    private final String function;

    FixedTermTypeSupport(String text, String function, String... supportTypes) {
        this.text = text;
        this.function = function;
        this.needValue = true;
        this.supportTypes = Sets.newHashSet(supportTypes);
    }

    FixedTermTypeSupport(String text, String function, boolean needValue, String... supportTypes) {
        this.text = text;
        this.function = function;
        this.needValue = needValue;
        this.supportTypes = Sets.newHashSet(supportTypes);
    }

    @Override
    public boolean isSupported(DataType type) {
        return supportTypes.isEmpty() || supportTypes.contains(type.getType());
    }

    protected Object convertValue(Object val, Term term) {
        if (val instanceof Collection) {
            //值为数组,则尝试获取第一个值
            if (((Collection<?>) val).size() == 1) {
                return ((Collection<?>) val).iterator().next();
            }
        }
        return val;
    }

    protected void appendFunction(String column, PrepareSqlFragments fragments) {
        if (needValue) {
            fragments.addSql(function + "(", column, ",");
        } else {
            fragments.addSql(function + "(", column, ")");
        }
    }

    @Override
    public SqlFragments createSql(String column, Object value, Term term) {
        PrepareSqlFragments fragments = PrepareSqlFragments.of();
        appendFunction(column, fragments);

        if (value instanceof NativeSql) {
            fragments
                .addSql(((NativeSql) value).getSql())
                .addParameter(((NativeSql) value).getParameters())
                .addSql(")");
        } else if (needValue) {
            value = convertValue(value, term);
            fragments
                .addSql("?")
                .addParameter(value)
                .addSql(")");
        }
        return fragments;
    }

    static String arrayToSpec(Object value) {
        if (value == null) {
            return "[]";
        }
        List<String> list = ConverterUtils
            .convertToList(value, String::valueOf);
        if (list.size() > 8) {
            return Stream
                .concat(list.stream().limit(8), Stream.of("..."))
                .collect(Collectors.joining(",", "[", "]"));
        }
        return list.toString();
    }

    @Override
    public String getType() {
        return name();
    }

    @Override
    public String getName() {
        return LocaleUtils.resolveMessage("message.term_type_" + name(), text);
    }

    protected String createValueDesc(Object expect) {
        return String.valueOf(expect);
    }

    protected String createDefaultDesc(String property, Object expect, Object actual) {
        return String.format("%s%s(%s)", property, getName(), expect);
    }

    @Override
    public String createDesc(String property, Object expect, Object actual) {
        //在国际化资源文件中查找对应的描述
        // {0}=属性名称,{1}=期望值
        //如: message.term_type_neq_desc={0}不等于{1}
        return LocaleUtils.resolveMessage(
            "message.term_type_" + name() + "_desc",
            createDefaultDesc(property, expect, expect),
            property,
            createValueDesc(expect),
            actual
        );
    }
}
