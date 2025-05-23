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
package org.jetlinks.community.tdengine;

import com.google.common.collect.Maps;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Point {

    private String metric;

    private String table;

    private Map<String, Object> values = Maps.newLinkedHashMapWithExpectedSize(32);

    private Map<String, Object> tags = Maps.newLinkedHashMapWithExpectedSize(8);

    private long timestamp;

    public Point(String metric, String table) {
        this.metric = metric;
        this.table = table;
    }

    public static Point of(String metric, String table) {
        return new Point(metric, table);
    }

    public Point timestamp(long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public Point tag(String metric, Object value) {
        tags.put(metric, value);
        return this;
    }

    public Point tags(Map<String, Object> values) {
        this.tags.putAll(values);
        return this;
    }

    public Point value(String metric, Object value) {
        values.put(metric, value);
        return this;
    }

    public Point values(Map<String, Object> values) {
        this.values.putAll(values);
        return this;
    }
}
