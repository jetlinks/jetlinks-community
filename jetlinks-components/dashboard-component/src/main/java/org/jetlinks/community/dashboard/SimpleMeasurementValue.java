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
package org.jetlinks.community.dashboard;

import lombok.*;
import org.hswebframework.utils.time.DateFormatter;

import java.util.Date;

@Getter
@Setter
@Builder
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class SimpleMeasurementValue implements MeasurementValue {

    private Object value;

    private String timeString;

    private long timestamp;

    public static SimpleMeasurementValue of(Object value, Date time) {
        return of(value, DateFormatter.toString(time, "yyyy-MM-dd HH:mm:ss"), time.getTime());
    }

    public static SimpleMeasurementValue of(Object value, long time) {
        return of(value, new Date(time));
    }
}
