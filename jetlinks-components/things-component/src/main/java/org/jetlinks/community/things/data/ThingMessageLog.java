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
package org.jetlinks.community.things.data;

import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.timeseries.TimeSeriesData;
import org.jetlinks.community.utils.ObjectMappers;

import java.io.Serializable;

@Getter
@Setter
@Generated
public class ThingMessageLog implements Serializable {

    private String id;

    private String thingId;

    private long createTime;

    private long timestamp;

    private ThingLogType type;

    private String content;

    public static ThingMessageLog of(TimeSeriesData data, String thingIdProperty) {
        ThingMessageLog log = data.as(ThingMessageLog.class);
        log.thingId = data.getString(thingIdProperty, log.thingId);
        return log;
    }

    @Override
    public String toString() {
        return ObjectMappers.toJsonString(this);
    }
}
