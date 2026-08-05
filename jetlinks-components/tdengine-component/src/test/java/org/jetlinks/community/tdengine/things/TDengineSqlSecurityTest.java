/*
 * Copyright 2026 JetLinks https://www.jetlinks.cn
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
package org.jetlinks.community.tdengine.things;

import org.jetlinks.community.things.data.PropertyAggregation;
import org.jetlinks.community.timeseries.query.Aggregation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TDengineSqlSecurityTest {

    @Test
    void shouldUseInternalAliasInRowMode() {
        PropertyAggregation aggregation = new PropertyAggregation(
            "temperature",
            "probe`, current_user as `db_user",
            Aggregation.AVG
        );

        String sql = TDengineRowModeQueryOperations.createAggregationColumn(aggregation, "__agg_0");

        assertEquals("avg(`numberValue`) `__agg_0`", sql);
        assertFalse(sql.contains(aggregation.getAlias()));
    }

    @Test
    void shouldKeepCountValueColumnInRowMode() {
        PropertyAggregation aggregation = new PropertyAggregation(
            "temperature",
            "count",
            Aggregation.COUNT
        );

        String sql = TDengineRowModeQueryOperations.createAggregationColumn(aggregation, "__agg_0");

        assertEquals("count(`value`) `__agg_0`", sql);
    }

    @Test
    void shouldEscapePropertyAndUseInternalAliasInColumnMode() {
        PropertyAggregation aggregation = new PropertyAggregation(
            "temperature`), current_user --",
            "probe`, current_user as `db_user",
            Aggregation.MAX
        );

        String sql = TDengineColumnModeQueryOperations.createAggregationColumn(aggregation, "__agg_1");

        assertEquals("max(`temperature``), current_user --`) `__agg_1`", sql);
        assertFalse(sql.contains(aggregation.getAlias()));
    }
}
