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

import org.hswebframework.ezorm.core.param.Sort;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.jetlinks.community.timeseries.TimeSeriesData;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public final class IoTDBQueryUtils {

    private IoTDBQueryUtils() {
    }

    public static String buildSelectSql(String metric, QueryParamEntity param, String database) {
        return buildSelectSql(metric, param, (List<Term>) null, database);
    }

    public static String buildSelectSql(String metric,
                                        QueryParamEntity param,
                                        List<Term> extraTerms,
                                        String database) {
        StringBuilder sql = new StringBuilder("SELECT *, \"time\" AS \"_ts\" FROM ")
            .append(table(database, metric));

        appendWhere(sql, param, extraTerms);
        sql.append(buildOrderBy(metric, param));

        if (param != null && param.isPaging()) {
            int pageSize = param.getPageSize();
            int pageIndex = param.getPageIndex();
            if (pageSize < 0 || pageIndex < 0) {
                throw new IllegalArgumentException("Page size and page index must not be negative");
            }
            long offset = (long) pageSize * pageIndex;
            sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);
        }
        return sql.toString();
    }

    public static String buildCountSql(String metric, QueryParamEntity param, String database) {
        return buildCountSql(metric, param, (List<Term>) null, database);
    }

    public static String buildCountSql(String metric,
                                       QueryParamEntity param,
                                       List<Term> extraTerms,
                                       String database) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS \"total\" FROM ")
            .append(table(database, metric));
        appendWhere(sql, param, extraTerms);
        return sql.toString();
    }

    public static String buildOrderBy(String metric, QueryParamEntity param) {
        List<Sort> sorts = param == null ? null : param.getSorts();
        if (sorts == null || sorts.isEmpty()) {
            return " ORDER BY \"time\" DESC";
        }

        StringJoiner orderBy = new StringJoiner(", ", " ORDER BY ", "");
        boolean hasSort = false;
        for (Sort sort : sorts) {
            if (sort == null || sort.getName() == null || sort.getName().trim().isEmpty()) {
                continue;
            }
            String direction = sort.getOrder() == null
                ? "ASC"
                : sort.getOrder().trim().toUpperCase(Locale.ROOT);
            if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
                throw new IllegalArgumentException("Unsupported sort order: " + sort.getOrder());
            }

            String column = IoTDBSqlUtils.encodeColumnName(sort.getName());
            orderBy.add(IoTDBSqlUtils.quoteQualifiedIdentifier(column) + " " + direction);
            hasSort = true;
        }
        return hasSort ? orderBy.toString() : " ORDER BY \"time\" DESC";
    }

    public static TimeSeriesData convertToTimeSeriesData(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        Object timestamp = timeValue(row, "_ts");
        if (timestamp == null) {
            timestamp = timeValue(row, "time");
        }
        if (timestamp == null) {
            timestamp = timeValue(row, "timestamp");
        }
        long epochMillis = toEpochMillis(timestamp);

        Map<String, Object> data = new HashMap<>(row);
        data.keySet().removeIf(IoTDBQueryUtils::isTimeResultColumn);
        Map<String, Object> decodedData = new HashMap<>(data.size());
        data.forEach((name, value) -> decodedData.put(IoTDBSqlUtils.decodeColumnName(name), value));
        return TimeSeriesData.of(epochMillis, decodedData);
    }

    private static Object timeValue(Map<String, Object> row, String column) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && column.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isTimeResultColumn(String column) {
        return column != null
            && ("_ts".equalsIgnoreCase(column)
                || "time".equalsIgnoreCase(column)
                || "timestamp".equalsIgnoreCase(column));
    }

    private static void appendWhere(StringBuilder sql,
                                    QueryParamEntity param,
                                    List<Term> extraTerms) {
        String where = param == null ? "" : IoTDBSqlUtils.buildWhere(param.getTerms());
        String extraWhere = IoTDBSqlUtils.buildWhere(extraTerms);
        if (where.isEmpty() && extraWhere.isEmpty()) {
            return;
        }

        sql.append(" WHERE ");
        if (!where.isEmpty()) {
            sql.append(where.substring("WHERE ".length()));
        }
        if (!extraWhere.isEmpty()) {
            if (!where.isEmpty()) {
                sql.append(" AND ");
            }
            sql.append(extraWhere.substring("WHERE ".length()));
        }
    }

    private static String table(String database, String metric) {
        if (database == null || database.trim().isEmpty()) {
            throw new IllegalArgumentException("IoTDB database must not be empty");
        }
        if (metric == null || metric.trim().isEmpty()) {
            throw new IllegalArgumentException("IoTDB table must not be empty");
        }
        return IoTDBSqlUtils.quoteIdentifier(database)
            + "."
            + IoTDBSqlUtils.quoteIdentifier(IoTDBSqlUtils.encodeMetricName(metric));
    }

    private static long toEpochMillis(Object timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("IoTDB query result is missing its time column");
        }
        if (timestamp instanceof Number) {
            return ((Number) timestamp).longValue();
        }
        if (timestamp instanceof Date) {
            return ((Date) timestamp).getTime();
        }
        if (timestamp instanceof Instant) {
            return ((Instant) timestamp).toEpochMilli();
        }
        if (timestamp instanceof OffsetDateTime) {
            return ((OffsetDateTime) timestamp).toInstant().toEpochMilli();
        }
        if (timestamp instanceof ZonedDateTime) {
            return ((ZonedDateTime) timestamp).toInstant().toEpochMilli();
        }
        if (timestamp instanceof LocalDateTime) {
            return ((LocalDateTime) timestamp).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        String value = String.valueOf(timestamp).trim();
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignore) {
            // Try the timestamp formats returned by IoTDB JDBC/session clients.
        }
        try {
            return Instant.parse(value).toEpochMilli();
        } catch (RuntimeException ignore) {
            // Continue with offset and local SQL timestamp formats.
        }
        try {
            return OffsetDateTime.parse(value).toInstant().toEpochMilli();
        } catch (RuntimeException ignore) {
            // Continue with the next supported timestamp format.
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'))
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        } catch (RuntimeException ignore) {
            // Timestamp.valueOf accepts SQL timestamps without a timezone.
        }
        try {
            return Timestamp.valueOf(value).getTime();
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Unsupported IoTDB time value: " + value, error);
        }
    }
}
