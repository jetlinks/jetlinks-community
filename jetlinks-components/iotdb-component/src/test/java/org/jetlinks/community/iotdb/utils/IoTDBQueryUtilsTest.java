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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IoTDBQueryUtilsTest {

    @Test
    void shouldBuildSelectAndCountSqlWithSeparatedFilters() {
        QueryParamEntity param = QueryParamEntity.of();
        param.setTerms(Collections.singletonList(term("name", "eq", "sensor")));
        param.setPaging(true);
        param.setPageSize(10);
        param.setPageIndex(2);
        Term extra = term("deviceId", "eq", "device-1");

        assertEquals("SELECT *, \"time\" AS \"_ts\" FROM \"db\".\"m_6d6574726963\" "
                         + "WHERE (\"c_6e616d65\" = 'sensor') AND (\"c_6465766963654964\" = 'device-1') "
                         + "ORDER BY \"time\" DESC LIMIT 10 OFFSET 20",
                     IoTDBQueryUtils.buildSelectSql("metric", param, List.of(extra), "db"));
        assertEquals("SELECT COUNT(*) AS \"total\" FROM \"db\".\"m_6d6574726963\" "
                         + "WHERE (\"c_6e616d65\" = 'sensor') AND (\"c_6465766963654964\" = 'device-1')",
                     IoTDBQueryUtils.buildCountSql("metric", param, List.of(extra), "db"));
    }

    @Test
    void shouldQuoteIdentifiersAndUseDefaultTimeSort() {
        QueryParamEntity param = QueryParamEntity.of();
        param.setPaging(false);
        assertEquals("SELECT *, \"time\" AS \"_ts\" FROM \"db\".\"m_6d657472696373\" ORDER BY \"time\" DESC",
                     IoTDBQueryUtils.buildSelectSql("metrics", param, "db"));
        assertEquals("SELECT *, \"time\" AS \"_ts\" FROM \"db; DROP TABLE x\".\"m_6d657472696373\" ORDER BY \"time\" DESC",
                     IoTDBQueryUtils.buildSelectSql("metrics", param, "db; DROP TABLE x"));
        assertEquals("SELECT *, \"time\" AS \"_ts\" FROM \"db.part\".\"m_6d657472696373\" ORDER BY \"time\" DESC",
                     IoTDBQueryUtils.buildSelectSql("metrics", param, "db.part"));
        assertThrows(IllegalArgumentException.class,
                     () -> IoTDBQueryUtils.buildSelectSql("", param, "db"));
    }

    @Test
    void shouldMapTimestampSortAndRejectInvalidDirections() {
        Sort sort = mock(Sort.class);
        when(sort.getName()).thenReturn("timestamp");
        when(sort.getOrder()).thenReturn("desc");
        QueryParamEntity param = QueryParamEntity.of();
        param.setSorts(Collections.singletonList(sort));
        assertEquals(" ORDER BY \"time\" DESC", IoTDBQueryUtils.buildOrderBy("metric", param));

        when(sort.getOrder()).thenReturn("desc; DROP TABLE metric");
        assertThrows(IllegalArgumentException.class, () -> IoTDBQueryUtils.buildOrderBy("metric", param));
    }

    @Test
    void shouldKeepNegativePageIndexFromProducingNegativeOffset() {
        QueryParamEntity param = QueryParamEntity.of();
        param.setPaging(true);
        param.setPageSize(10);
        param.setPageIndex(-1);
        assertEquals("SELECT *, \"time\" AS \"_ts\" FROM \"db\".\"m_6d6574726963\" "
                         + "ORDER BY \"time\" DESC LIMIT 10 OFFSET 0",
                     IoTDBQueryUtils.buildSelectSql("metric", param, "db"));
    }

    @Test
    void shouldConvertIoTDBTimeValuesWithoutMutatingRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("_ts", 1700000000000L);
        row.put("time", 1700000000000L);
        row.put("c_74656d7065726174757265", 21.5);

        TimeSeriesData data = IoTDBQueryUtils.convertToTimeSeriesData(row);
        assertEquals(1700000000000L, data.getTimestamp());
        assertEquals(Collections.singletonMap("temperature", 21.5), data.getData());
        assertEquals(3, row.size());
        assertEquals(1700000000000L,
                     IoTDBQueryUtils.convertToTimeSeriesData(Collections.singletonMap("time", new Date(1700000000000L)))
                         .getTimestamp());
        assertEquals(1700000000000L,
                     IoTDBQueryUtils.convertToTimeSeriesData(Collections.singletonMap(
                         "_ts", Instant.ofEpochMilli(1700000000000L))).getTimestamp());
        assertEquals(Collections.singletonMap("temperature", 21.5),
                     IoTDBQueryUtils.convertToTimeSeriesData(Map.of(
                         "_TS", 1700000000000L,
                         "TIME", 1700000000000L,
                         "c_74656d7065726174757265", 21.5)).getData());
        assertThrows(IllegalArgumentException.class,
                     () -> IoTDBQueryUtils.convertToTimeSeriesData(Collections.singletonMap("time", "invalid")));
    }

    private static Term term(String column, String termType, Object value) {
        Term term = new Term();
        term.setColumn(column);
        term.setTermType(termType);
        term.setValue(value);
        return term;
    }
}
