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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IoTDBSqlUtilsTest {

    @Test
    void shouldQuoteIdentifiersSafely() {
        assertEquals("\"table\"", IoTDBSqlUtils.quoteIdentifier("table"));
        assertEquals("\"part\".\"table\"", IoTDBSqlUtils.quoteQualifiedIdentifier("part.table"));
        assertEquals("\"a\"\"b\"", IoTDBSqlUtils.quoteIdentifier("a\"b"));
        assertThrows(IllegalArgumentException.class, () -> IoTDBSqlUtils.quoteQualifiedIdentifier("db..table"));
    }

    @Test
    void shouldEncodeAndDecodeLogicalNamesReversibly() {
        assertEquals("c_74656d7065726174757265", IoTDBSqlUtils.encodeColumnName("temperature"));
        assertEquals("temperature", IoTDBSqlUtils.decodeColumnName("c_74656d7065726174757265"));
        assertEquals("c_e6b8a9e5baa6", IoTDBSqlUtils.encodeColumnName("温度"));
        assertEquals("温度", IoTDBSqlUtils.decodeColumnName("c_e6b8a9e5baa6"));
        assertEquals("time", IoTDBSqlUtils.encodeColumnName("timestamp"));
        assertEquals("m_6d6574726963", IoTDBSqlUtils.encodeMetricName("metric"));
        assertEquals("metric", IoTDBSqlUtils.decodeMetricName("m_6d6574726963"));
        assertThrows(IllegalArgumentException.class, () -> IoTDBSqlUtils.decodeColumnName("c_abc"));
    }

    @Test
    void shouldEncodeSqlLiterals() {
        assertEquals("'O''Reilly'", IoTDBSqlUtils.literal("O'Reilly"));
        assertEquals("42", IoTDBSqlUtils.literal(42));
        assertEquals("true", IoTDBSqlUtils.literal(true));
        assertEquals("'2024-01-02T03:04:05Z'", IoTDBSqlUtils.literal(Instant.parse("2024-01-02T03:04:05Z")));
        assertEquals("'1970-01-01T00:00:00Z'", IoTDBSqlUtils.literal(new Date(0)));
        assertEquals("('a', 2)", IoTDBSqlUtils.literal(Arrays.asList("a", 2)));
    }

    @Test
    void shouldBuildComparisonAndTimePredicates() {
        assertEquals(
            "WHERE (\"c_6e616d65\" = 'sensor' AND \"time\" >= 1700000000000 AND \"time\" < 1800000000000)",
            IoTDBSqlUtils.buildWhere(Arrays.asList(
                term("name", "eq", "sensor"),
                term("timestamp", "gte", 1700000000000L),
                term("_ts", "lt", 1800000000000L))));
    }

    @Test
    void shouldBuildNullInAndRangePredicates() {
        assertEquals(
            "WHERE (\"c_76616c7565\" IS NULL AND \"c_7374617465\" IN ('ready', 'running') AND (\"c_76616c7565\" >= 1 AND \"c_76616c7565\" <= 5) AND (\"c_76616c7565\" < 1 OR \"c_76616c7565\" > 5))",
            IoTDBSqlUtils.buildWhere(Arrays.asList(
                term("value", "isnull", null),
                term("state", "in", Arrays.asList("ready", "running")),
                term("value", "btw", Arrays.asList(1, 5)),
                term("value", "nbtw", Arrays.asList(1, 5)))));
    }

    @Test
    void shouldBuildEverySupportedComparisonPredicate() {
        assertEquals(
            "WHERE (\"c_61\" = 1 AND \"c_62\" != 2 AND \"c_63\" > 3 AND \"c_64\" >= 4 AND \"c_65\" < 5 AND \"c_66\" <= 6 AND \"c_67\" LIKE 'x%' AND \"c_68\" NOT IN ('bad'))",
            IoTDBSqlUtils.buildWhere(Arrays.asList(
                term("a", "is", 1),
                term("b", "not", 2),
                term("c", "gt", 3),
                term("d", "gte", 4),
                term("e", "lt", 5),
                term("f", "lte", 6),
                term("g", "like", "x%"),
                term("h", "nin", Collections.singletonList("bad")))));
    }

    @Test
    void shouldGiveEmptyInSetsBooleanSemantics() {
        assertEquals("WHERE (1 = 0)",
                     IoTDBSqlUtils.buildWhere(Collections.singletonList(term("state", "in", Collections.emptyList()))));
        assertEquals("WHERE (1 = 1)",
                     IoTDBSqlUtils.buildWhere(Collections.singletonList(term("state", "nin", Collections.emptyList()))));
    }

    @Test
    void shouldConvertTimePredicatesToEpochMillis() {
        long timestamp = 1700000000123L;
        assertEquals("WHERE (\"time\" = 1700000000123 AND \"time\" >= 1700000000123)",
                     IoTDBSqlUtils.buildWhere(Arrays.asList(
                         term("timestamp", "eq", new Date(timestamp)),
                         term("_ts", "gte", Instant.ofEpochMilli(timestamp)))));
    }

    @Test
    void shouldRejectUnsupportedAndMalformedTerms() {
        assertThrows(IllegalArgumentException.class,
                     () -> IoTDBSqlUtils.buildWhere(Collections.singletonList(term("state", "contains", "ok"))));

        Term mixed = term("state", "eq", "ok");
        mixed.setTerms(Collections.singletonList(term("count", "gt", 0)));
        assertThrows(IllegalArgumentException.class,
                     () -> IoTDBSqlUtils.buildWhere(Collections.singletonList(mixed)));
    }

    @Test
    void shouldKeepNestedBooleanGroupsAndSkipInvalidTerms() {
        Term root = term("", "eq", null);
        root.setType(Term.Type.or);
        root.setTerms(Arrays.asList(term("status", "eq", "ok"), term("count", "gt", 0)));
        Term deleted = term("deleted", "notnull", null);
        deleted.setType(Term.Type.or);

        assertEquals("WHERE ((\"c_737461747573\" = 'ok' AND \"c_636f756e74\" > 0) OR \"c_64656c65746564\" IS NOT NULL)",
            IoTDBSqlUtils.buildWhere(Arrays.asList(root, deleted)));
        assertEquals("WHERE (\"c_76616c7565\" IS NULL)",
                     IoTDBSqlUtils.buildWhere(Collections.singletonList(term("value", "eq", null))));
        assertEquals("WHERE (\"c_76616c7565\" IS NOT NULL)",
                     IoTDBSqlUtils.buildWhere(Collections.singletonList(term("value", "not", null))));
        assertEquals("", IoTDBSqlUtils.buildWhere(Collections.emptyList()));
    }

    private static Term term(String column, String termType, Object value) {
        Term term = new Term();
        term.setColumn(column);
        term.setTermType(termType);
        term.setValue(value);
        return term;
    }
}
