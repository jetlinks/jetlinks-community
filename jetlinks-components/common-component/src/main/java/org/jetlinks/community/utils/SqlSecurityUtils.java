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

/**
 * 物数据组件中手写 SQL 片段的最小安全辅助工具。
 *
 * 仅处理数据库无法参数化的标识符和必须拼接到 native SQL 的字面量；业务查询值仍应优先使用
 * Query/Term DSL 或数据库驱动参数绑定。
 */
public final class SqlSecurityUtils {

    private SqlSecurityUtils() {
    }

    public static String aggregationAlias(int index) {
        return "__agg_" + index;
    }

    public static String quoteDouble(String identifier) {
        return quote(identifier, '"');
    }

    public static String quoteBacktick(String identifier) {
        return quote(identifier, '`');
    }

    public static String quoteSingleLiteral(String value) {
        return quote(value, '\'');
    }

    private static String quote(String value, char quote) {
        String text = value == null ? "" : value;
        String quoted = String.valueOf(quote);
        return quoted + text.replace(quoted, quoted + quoted) + quoted;
    }
}
