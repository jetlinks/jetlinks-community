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
package org.jetlinks.community.iotdb.utils;

import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.community.Interval;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public final class IoTDBSqlUtils {

    private static final String COLUMN_PREFIX = "c_";
    private static final String METRIC_PREFIX = "m_";

    private IoTDBSqlUtils() {
    }

    public static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier must not be empty");
        }
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    public static String quoteQualifiedIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier must not be empty");
        }
        String[] parts = identifier.split("\\.", -1);
        StringJoiner quoted = new StringJoiner(".");
        for (String part : parts) {
            if (part.isEmpty()) {
                throw new IllegalArgumentException("Qualified identifier contains an empty part: " + identifier);
            }
            quoted.add(quoteIdentifier(part));
        }
        return quoted.toString();
    }

    public static String encodeColumnName(String column) {
        if (column == null || column.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name must not be empty");
        }
        if (isTimeColumn(column)) {
            return "time";
        }
        return COLUMN_PREFIX + encodeHex(column);
    }

    public static String timeBucket(Interval interval) {
        long amount = interval.getNumber().longValueExact();
        if (amount <= 0) {
            throw new IllegalArgumentException("Time bucket must be positive");
        }
        String unit = interval.getExpression();
        switch (unit) {
            case Interval.quarter:
                amount = Math.multiplyExact(amount, 3);
                unit = "mo";
                break;
            case Interval.month: unit = "mo"; break;
            case Interval.millis: unit = "ms"; break;
            case Interval.year: case Interval.weeks: case Interval.days: case Interval.hours:
            case Interval.minutes: case Interval.seconds: break;
            default: throw new IllegalArgumentException("Unknown time bucket unit: " + unit);
        }
        return "date_bin(" + amount + unit + ", \"time\")";
    }

    public static String decodeColumnName(String column) {
        return decodeName(column, COLUMN_PREFIX);
    }

    public static String encodeMetricName(String metric) {
        if (metric == null || metric.trim().isEmpty()) {
            throw new IllegalArgumentException("Metric name must not be empty");
        }
        return METRIC_PREFIX + encodeHex(metric);
    }

    public static String decodeMetricName(String metric) {
        return decodeName(metric, METRIC_PREFIX);
    }

    private static boolean isTimeColumn(String column) {
        return "time".equalsIgnoreCase(column)
            || "timestamp".equalsIgnoreCase(column)
            || "_ts".equalsIgnoreCase(column);
    }

    private static String encodeHex(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length * 2);
        for (byte valueByte : bytes) {
            int unsigned = valueByte & 0xff;
            encoded.append(Character.forDigit(unsigned >>> 4, 16));
            encoded.append(Character.forDigit(unsigned & 0x0f, 16));
        }
        return encoded.toString();
    }

    private static String decodeName(String value, String prefix) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Encoded name must not be empty");
        }
        if (!value.startsWith(prefix)) {
            return value;
        }
        String hex = value.substring(prefix.length());
        if (hex.isEmpty() || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid encoded name: " + value);
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Invalid encoded name: " + value);
            }
            bytes[i] = (byte) ((high << 4) | low);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static String buildWhere(List<Term> terms) {
        String expression = buildTerms(terms);
        return expression.isEmpty() ? "" : "WHERE (" + expression + ")";
    }

    public static String literal(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String || value instanceof Character || value instanceof Enum<?>) {
            return quoteString(String.valueOf(value));
        }
        if (value instanceof Boolean) {
            return value.toString().toLowerCase(Locale.ROOT);
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }
        if (value instanceof Number) {
            if (value instanceof Double && !Double.isFinite((Double) value)
                || value instanceof Float && !Float.isFinite((Float) value)) {
                throw new IllegalArgumentException("Non-finite numbers are not valid SQL literals");
            }
            return value.toString();
        }
        if (value instanceof Date) {
            return quoteString(Instant.ofEpochMilli(((Date) value).getTime()).toString());
        }
        if (value instanceof TemporalAccessor) {
            return quoteString(value.toString());
        }
        if (value instanceof Collection<?>) {
            return literalCollection((Collection<?>) value);
        }
        if (value.getClass().isArray()) {
            List<Object> values = new ArrayList<>(Array.getLength(value));
            for (int i = 0; i < Array.getLength(value); i++) {
                values.add(Array.get(value, i));
            }
            return literalCollection(values);
        }
        return quoteString(value.toString());
    }

    private static String quoteString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String literalCollection(Collection<?> values) {
        StringJoiner sql = new StringJoiner(", ", "(", ")");
        for (Object value : values) {
            sql.add(literal(value));
        }
        return sql.toString();
    }

    private static String buildTerms(List<Term> terms) {
        if (terms == null || terms.isEmpty()) {
            return "";
        }

        StringBuilder sql = new StringBuilder();
        for (Term term : terms) {
            if (term == null) {
                continue;
            }
            String expression = buildTerm(term);
            if (expression.isEmpty()) {
                continue;
            }
            if (sql.length() > 0) {
                sql.append(term.getType() == Term.Type.or ? " OR " : " AND ");
            }
            sql.append(expression);
        }
        return sql.toString();
    }

    private static String buildTerm(Term term) {
        List<Term> children = term.getTerms();
        boolean hasColumn = term.getColumn() != null && !term.getColumn().trim().isEmpty();
        boolean hasChildren = children != null && !children.isEmpty();
        if (hasColumn && hasChildren) {
            throw new IllegalArgumentException("A term cannot have both a column and child terms");
        }
        if (!hasColumn && hasChildren) {
            String nested = buildTerms(children);
            return nested.isEmpty() ? "" : "(" + nested + ")";
        }
        if (!hasColumn) {
            return "";
        }
        if (term.getTermType() == null || term.getTermType().trim().isEmpty()) {
            throw new IllegalArgumentException("Term operator must not be empty for column: " + term.getColumn());
        }

        String operation = term.getTermType().toLowerCase(Locale.ROOT);
        boolean timeColumn = isTimeColumn(term.getColumn());
        String column = encodeColumnName(term.getColumn());
        String quotedColumn = quoteQualifiedIdentifier(column);
        Object value = term.getValue();

        switch (operation) {
            case "isnull":
                return quotedColumn + " IS NULL";
            case "notnull":
                return quotedColumn + " IS NOT NULL";
            case "eq":
            case "is":
                return value == null
                    ? quotedColumn + " IS NULL"
                    : quotedColumn + " = " + literal(value(value, timeColumn));
            case "not":
                return value == null
                    ? quotedColumn + " IS NOT NULL"
                    : quotedColumn + " != " + literal(value(value, timeColumn));
            case "gt":
                return value == null ? "" : quotedColumn + " > " + literal(value(value, timeColumn));
            case "gte":
                return value == null ? "" : quotedColumn + " >= " + literal(value(value, timeColumn));
            case "lt":
                return value == null ? "" : quotedColumn + " < " + literal(value(value, timeColumn));
            case "lte":
                return value == null ? "" : quotedColumn + " <= " + literal(value(value, timeColumn));
            case "like":
                return value == null ? "" : quotedColumn + " LIKE " + literal(value);
            case "btw":
                return buildBetween(quotedColumn, value, false, timeColumn);
            case "nbtw":
                return buildBetween(quotedColumn, value, true, timeColumn);
            case "in":
                return buildIn(quotedColumn, value, false, timeColumn);
            case "nin":
                return buildIn(quotedColumn, value, true, timeColumn);
            default:
                throw new IllegalArgumentException("Unsupported IoTDB term operator: " + term.getTermType());
        }
    }

    private static String buildBetween(String column, Object value, boolean negated, boolean timeColumn) {
        List<?> values = values(value, timeColumn);
        if (values.size() < 2 || values.get(0) == null || values.get(1) == null) {
            return "";
        }
        String left = literal(values.get(0));
        String right = literal(values.get(1));
        return negated
            ? "(" + column + " < " + left + " OR " + column + " > " + right + ")"
            : "(" + column + " >= " + left + " AND " + column + " <= " + right + ")";
    }

    private static String buildIn(String column, Object value, boolean negated, boolean timeColumn) {
        List<?> values = values(value, timeColumn);
        if (values.isEmpty()) {
            return negated ? "1 = 1" : "1 = 0";
        }
        StringJoiner literals = new StringJoiner(", ", "(", ")");
        for (Object item : values) {
            literals.add(literal(item));
        }
        return column + (negated ? " NOT IN " : " IN ") + literals;
    }

    private static List<?> values(Object value, boolean timeColumn) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?>) {
            List<Object> values = new ArrayList<>(((Collection<?>) value).size());
            for (Object item : (Collection<?>) value) {
                values.add(value(item, timeColumn));
            }
            return values;
        }
        if (value.getClass().isArray()) {
            List<Object> values = new ArrayList<>(Array.getLength(value));
            for (int i = 0; i < Array.getLength(value); i++) {
                values.add(value(Array.get(value, i), timeColumn));
            }
            return values;
        }
        if (value instanceof String) {
            String[] parts = ((String) value).split(",", -1);
            List<Object> values = new ArrayList<>(parts.length);
            for (String part : parts) {
                values.add(value(part.trim(), timeColumn));
            }
            return values;
        }
        return List.of(value(value, timeColumn));
    }

    private static Object value(Object value, boolean timeColumn) {
        if (!timeColumn || value == null) {
            return value;
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof Instant) {
            return ((Instant) value).toEpochMilli();
        }
        if (value instanceof OffsetDateTime) {
            return ((OffsetDateTime) value).toInstant().toEpochMilli();
        }
        if (value instanceof ZonedDateTime) {
            return ((ZonedDateTime) value).toInstant().toEpochMilli();
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        if (value instanceof TemporalAccessor) {
            try {
                return Instant.from((TemporalAccessor) value).toEpochMilli();
            } catch (RuntimeException ignore) {
                return value;
            }
        }
        return value;
    }
}
