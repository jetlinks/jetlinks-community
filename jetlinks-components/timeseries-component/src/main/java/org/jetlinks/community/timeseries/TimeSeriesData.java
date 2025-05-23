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
package org.jetlinks.community.timeseries;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.ValueObject;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * 时序数据封装类
 *
 * @author zhouhao
 */
public interface TimeSeriesData extends ValueObject {

    static TimeSeriesData of(Date date, Map<String, Object> data) {
        return of(date == null ? System.currentTimeMillis() : date.getTime(), data);
    }

    static TimeSeriesData of(long timestamp, Map<String, Object> data) {
        return new SimpleTimeSeriesData(timestamp, data);
    }

    long getTimestamp();

    Map<String, Object> getData();

    @Override
    default Map<String, Object> values() {
        return getData();
    }

    @Override
    default Optional<Object> get(String name) {
        return Optional.ofNullable(getData().get(name));
    }

    @Override
    default <T> T as(Class<T> type) {
        return FastBeanCopier.copy(getData(), type);
    }
}
