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
package org.jetlinks.community;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.TimeUtils;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

public interface ValueObject {

    Map<String, Object> values();

    default Optional<Object> get(String name) {
        return Optional.ofNullable(values())
                       .map(map -> map.get(name));
    }

    default Optional<Integer> getInt(String name) {
        return get(name, Integer.class);
    }

    default int getInt(String name, int defaultValue) {
        return getInt(name).orElse(defaultValue);
    }

    default Optional<Long> getLong(String name) {
        return get(name, Long.class);
    }

    default long getLong(String name, long defaultValue) {
        return getLong(name).orElse(defaultValue);
    }

    default Optional<Duration> getDuration(String name) {
        return getString(name)
            .map(TimeUtils::parse);
    }

    default Optional<Interval> getInterval(String name) {
        return getString(name)
            .map(Interval::of);
    }

    default Interval getInterval(String name, Interval defaultValue) {
        return getInterval(name)
            .orElse(defaultValue);
    }

    default Duration getDuration(String name, Duration defaultValue) {
        return getDuration(name)
            .orElse(defaultValue);
    }

    default Optional<Date> getDate(String name) {
        return this
            .get(name)
            .map(d -> {
                if (d instanceof Date) {
                    return (Date) d;
                }
                return TimeUtils.parseDate(String.valueOf(d));
            });
    }

    default Date getDate(String name, Date defaultValue) {
        return getDate(name).orElse(defaultValue);
    }

    default Optional<Double> getDouble(String name) {
        return get(name, Double.class);
    }

    default double getDouble(String name, double defaultValue) {
        return getDouble(name).orElse(defaultValue);
    }

    default Optional<String> getString(String name) {
        return get(name, String.class)
            .filter(StringUtils::hasText);
    }

    default String getString(String name, String defaultValue) {
        return getString(name).orElse(defaultValue);
    }

    default Optional<Boolean> getBoolean(String name) {
        return get(name)
            .map(CastUtils::castBoolean);
    }

    default boolean getBoolean(String name, boolean defaultValue) {
        return getBoolean(name).orElse(defaultValue);
    }

    default <T> Optional<T> get(String name, Class<T> type) {
        return get(name)
            .map(obj -> FastBeanCopier.DEFAULT_CONVERT.convert(obj, type, FastBeanCopier.EMPTY_CLASS_ARRAY));
    }

    @SuppressWarnings("unchecked")
    static ValueObject of(Map<String, ?> mapVal) {
        return () -> (Map<String, Object>) mapVal;
    }

    default <T> T as(Class<T> type) {
        return FastBeanCopier.copy(values(), type);
    }
}
