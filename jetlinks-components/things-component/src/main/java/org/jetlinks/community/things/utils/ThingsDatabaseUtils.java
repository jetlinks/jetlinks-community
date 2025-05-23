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
package org.jetlinks.community.things.utils;

import org.hswebframework.ezorm.core.ValueCodec;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.ezorm.rdb.codec.*;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.core.metadata.Converter;
import org.jetlinks.core.metadata.DataType;
import org.jetlinks.core.metadata.PropertyMetadata;
import org.jetlinks.core.metadata.SimplePropertyMetadata;
import org.jetlinks.core.metadata.types.*;
import org.jetlinks.core.utils.StringBuilderUtils;
import org.jetlinks.community.ConfigMetadataConstants;
import org.jetlinks.community.utils.ConverterUtils;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.supports.official.DeviceMetadataParser;
import org.springframework.core.ResolvableType;
import org.springframework.util.ObjectUtils;

import java.sql.JDBCType;
import java.sql.SQLType;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class ThingsDatabaseUtils {

    static GeoCodec geoCodec = new GeoCodec();

    static StringCodec stringCodec = new StringCodec();

    public static DataType sqlTypeToDataType(SQLType sqlType) {
        if (sqlType == JDBCType.BIGINT) {
            return LongType.GLOBAL;
        }
        if (sqlType == JDBCType.INTEGER || sqlType == JDBCType.SMALLINT || sqlType == JDBCType.TINYINT) {
            return IntType.GLOBAL;
        }
        if (sqlType == JDBCType.TIMESTAMP || sqlType == JDBCType.DATE || sqlType == JDBCType.TIME) {
            return DateTimeType.GLOBAL;
        }
        if (sqlType == JDBCType.DOUBLE || sqlType == JDBCType.FLOAT || sqlType == JDBCType.NUMERIC || sqlType == JDBCType.DECIMAL) {
            return DoubleType.GLOBAL;
        }
        return StringType.GLOBAL;
    }

    static class GeoCodec implements ValueCodec<String, GeoPoint> {

        @Override
        public String encode(Object value) {
            return String.valueOf(value);
        }

        @Override
        public GeoPoint decode(Object data) {
            return GeoPoint.of(data);
        }
    }

    static class StringCodec implements ValueCodec<String, String> {

        @Override
        public String encode(Object value) {
            return String.valueOf(value);
        }

        @Override
        public String decode(Object data) {
            return String.valueOf(data);
        }
    }

    public static Class<?> convertJavaType(DataType dataType) {
        if (null == dataType) {
            return Map.class;
        }
        switch (dataType.getType()) {
            case IntType.ID:
                return Integer.class;
            case LongType.ID:
                return Long.class;
            case FloatType.ID:
                return Float.class;
            case DoubleType.ID:
                return Double.class;
            case BooleanType.ID:
                return Boolean.class;
            case DateTimeType.ID:
                return Date.class;
            case ArrayType.ID:
                return List.class;
            case GeoType.ID:
            case ObjectType.ID:
                return Map.class;
            default:
                return String.class;
        }
    }

    public static RDBColumnMetadata convertColumn(PropertyMetadata metadata, RDBColumnMetadata column) {
        column.setName(metadata.getId());
        column.setComment(metadata.getName());
        DataType type = metadata.getValueType();
        if (type instanceof NumberType) {
            column.setLength(32);
            column.setPrecision(32);
            if (type instanceof DoubleType) {
                column.setScale(Optional.ofNullable(((DoubleType) type).getScale()).orElse(4));
                column.setValueCodec(new NumberValueCodec(Double.class));
                column.setJdbcType(JDBCType.DOUBLE, Double.class);
            } else if (type instanceof FloatType) {
                column.setScale(Optional.ofNullable(((FloatType) type).getScale()).orElse(2));
                column.setValueCodec(new NumberValueCodec(Float.class));
                column.setJdbcType(JDBCType.FLOAT, Float.class);
            } else if (type instanceof LongType) {
                column.setValueCodec(new NumberValueCodec(Long.class));
                column.setJdbcType(JDBCType.BIGINT, Long.class);
            } else {
                column.setValueCodec(new NumberValueCodec(IntType.class));
                column.setJdbcType(JDBCType.NUMERIC, Integer.class);
            }
        } else if (type instanceof ObjectType) {
            column.setJdbcType(JDBCType.CLOB, String.class);
            column.setValueCodec(JsonValueCodec.of(Map.class));
        } else if (type instanceof ArrayType) {
            column.setJdbcType(JDBCType.CLOB, String.class);
            ArrayType arrayType = ((ArrayType) type);
            column.setValueCodec(JsonValueCodec.ofCollection(ArrayList.class, convertJavaType(arrayType.getElementType())));
        } else if (type instanceof DateTimeType) {
            column.setJdbcType(JDBCType.BIGINT, Long.class);
            column.setValueCodec(new NumberValueCodec(Long.class));
        } else if (type instanceof GeoType) {
            column.setJdbcType(JDBCType.VARCHAR, String.class);
            column.setValueCodec(geoCodec);
            column.setLength(128);
        } else if (type instanceof EnumType) {
            column.setJdbcType(JDBCType.VARCHAR, String.class);
            column.setValueCodec(stringCodec);
            column.setLength(64);
        } else if (type instanceof BooleanType) {
            column.setJdbcType(JDBCType.BOOLEAN, Boolean.class);
            column.setValueCodec(new BooleanValueCodec(JDBCType.BOOLEAN));
            column.setLength(64);
        } else {
            int len = type
                .getExpand(ConfigMetadataConstants.maxLength.getKey())
                .filter(o -> !ObjectUtils.isEmpty(o))
                .map(CastUtils::castNumber)
                .map(Number::intValue)
                .orElse(255);
            if (len < 0 || len > 2048) {
                column.setJdbcType(JDBCType.LONGVARCHAR, String.class);
                column.setValueCodec(ClobValueCodec.INSTANCE);
            } else {
                column.setJdbcType(JDBCType.VARCHAR, String.class);
                column.setLength(len == 0 ? 255 : len);
                column.setValueCodec(stringCodec);
            }
        }

        return column;
    }

    public static RDBColumnMetadata convertColumn(PropertyMetadata metadata) {
        return convertColumn(metadata, new RDBColumnMetadata());
    }

    public static PropertyMetadata convertMetadata(RDBColumnMetadata column) {
        return SimplePropertyMetadata.of(column.getAlias(), column.getComment(), convertDataType(column));
    }

    public static DataType convertDataType(RDBColumnMetadata column) {
        DataType type;
        if (column.getJavaType() != null) {
            type = DeviceMetadataParser.withType(ResolvableType.forType(column.getJavaType()));
        } else if (column.getSqlType() != null) {
            type = ThingsDatabaseUtils.sqlTypeToDataType(column.getSqlType());
        } else {
            type = StringType.GLOBAL;
        }
        return type;
    }

    private final static Base64.Encoder tableEncoder = Base64.getUrlEncoder().withoutPadding();


    public static String createTableName(String prefix, String... suffixes) {
        int len = prefix.length();

        for (String suffix : suffixes) {
            len += suffix.length();
        }

        if (len >= 64 - suffixes.length) {
            //表名太长，使用短编码
            return createTableName0(
                prefix, tableEncoder.encodeToString(DigestUtils.md5(String.join("_", suffixes)))
            );
        }

        return createTableName0(prefix, suffixes);
    }

    private static String createTableName0(String prefix, String... suffixes) {
        return StringBuilderUtils
            .buildString(prefix, suffixes, (_prefix, _suffixes, builder) -> {

                //前缀
                appendTable(_prefix, builder);

                //后缀
                for (String suffix : _suffixes) {
                    builder.append('_');
                    appendTable(suffix, builder);
                }
            });
    }

    private static void appendTable(String table, StringBuilder builder) {
        for (int i = 0; i < table.length(); i++) {
            char ch = Character.toLowerCase(table.charAt(i));
            if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9')) {
                builder.append('_');
            } else {
                builder.append(ch);
            }
        }
    }

    public static Object tryConvertTermValue(DataType type, Object value) {
        if (type instanceof DateTimeType) {
            return TimeUtils.convertToDate(value).getTime();
        } else if (type instanceof Converter) {
            return ((Converter<?>) type).convert(value);
        }
        return value;
    }

    public static void tryConvertTermValue(DataType type,
                                           Term term,
                                           BiFunction<DataType, Object, Object> tryConvertTermValue) {
        tryConvertTermValue(type,
                            term,
                            ThingsDatabaseUtils::isDoNotConvertValue,
                            ThingsDatabaseUtils::maybeList,
                            tryConvertTermValue);
    }

    public static void tryConvertTermValue(DataType type,
                                           Term term) {
        tryConvertTermValue(type,
                            term,
                            ThingsDatabaseUtils::tryConvertTermValue);
    }

    public static void tryConvertTermValue(DataType type,
                                           Term term,
                                           BiPredicate<DataType, Term> isDoNotConvertValue,
                                           BiPredicate<DataType, Term> maybeIsList,
                                           BiFunction<DataType, Object, Object> tryConvertTermValue) {
        if (ObjectUtils.isEmpty(term.getColumn()) || isDoNotConvertValue.test(type, term)) {
            return;
        }

        Object value;
        if (maybeIsList.test(type, term)) {
            value = ConverterUtils.tryConvertToList(term.getValue(), v -> tryConvertTermValue.apply(type, v));
        } else {
            value = tryConvertTermValue.apply(type, term.getValue());
        }
        if (null != value) {
            term.setValue(value);
        }
    }

    public static boolean maybeList(DataType type, Term term) {
        switch (term.getTermType().toLowerCase()) {
            case TermType.in:
            case TermType.nin:
            case TermType.btw:
            case TermType.nbtw:
                return true;
        }
        return false;
    }

    public static boolean isDoNotConvertValue(DataType type, Term term) {
        switch (term.getTermType().toLowerCase()) {
            case TermType.isnull:
            case TermType.notnull:
            case TermType.empty:
            case TermType.nempty:
                return true;
        }
        return false;
    }


}
