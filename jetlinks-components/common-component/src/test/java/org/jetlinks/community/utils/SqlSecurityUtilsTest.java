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
package org.jetlinks.community.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlSecurityUtilsTest {

    @Test
    void shouldEscapeSqlIdentifierQuotes() {
        assertEquals("\"a\"\"b\"", SqlSecurityUtils.quoteDouble("a\"b"));
        assertEquals("`a``b`", SqlSecurityUtils.quoteBacktick("a`b"));
    }

    @Test
    void shouldEscapeSingleQuotedLiteral() {
        assertEquals("'cpu'' or 1=1 --'", SqlSecurityUtils.quoteSingleLiteral("cpu' or 1=1 --"));
    }

    @Test
    void shouldGenerateStableInternalAggregationAlias() {
        assertEquals("__agg_0", SqlSecurityUtils.aggregationAlias(0));
        assertEquals("__agg_12", SqlSecurityUtils.aggregationAlias(12));
    }
}
