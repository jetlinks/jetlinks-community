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
package org.jetlinks.community.timeseries.query;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.community.Interval;

import java.time.Duration;

@Getter
@Setter
@NoArgsConstructor
public class TimeGroup extends Group{


    /**
     * 时间分组间隔,如: 1d , 30s
     */
    private Interval interval;

    /**
     * 时序时间返回格式  如 YYYY-MM-dd
     */
    private String format;

    /**
     * 时间分组偏移量，部分实现支持偏移量分组.
     */
    private long offset;

    public TimeGroup(Interval interval, String alias, String format) {
        super("timestamp", alias);
        this.interval = interval;
        this.format = format;
    }

    public static TimeGroup of(Interval interval, String alias, String format) {
        return new TimeGroup(interval, alias, format);
    }

    public TimeGroup offset(Duration duration) {
        offset = duration.toMillis();
        return this;
    }
}
