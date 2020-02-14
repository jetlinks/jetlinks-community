package org.jetlinks.community;

import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.utils.TimeUtils;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;

public interface ValueObject {

    Optional<Object> get(String name);

    default Optional<Integer> getInt(String name) {
        return get(name, Integer.class);
    }

    default Optional<Long> getLong(String name) {
        return get(name, Long.class);
    }

    default Optional<Duration> getDuration(String name) {
        return getString(name)
            .map(TimeUtils::parse);
    }

    default Optional<Date> getDate(String name) {
        return get(name, Date.class);
    }

    default Optional<Double> getDouble(String name) {
        return get(name, Double.class);
    }

    default Optional<String> getString(String name) {
        return get(name, String.class);
    }

    default Optional<Boolean> getBoolean(String name) {
        return get(name, Boolean.class);
    }

    default <T> Optional<T> get(String name, Class<T> type) {
        return get(name)
            .map(obj -> FastBeanCopier.DEFAULT_CONVERT.convert(obj, type, FastBeanCopier.EMPTY_CLASS_ARRAY));
    }

}
