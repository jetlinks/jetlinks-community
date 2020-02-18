package org.jetlinks.community.timeseries.micrometer;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;

import java.util.regex.Pattern;

public class ElasticNamingConvention implements NamingConvention {

    private static final Pattern FIRST_UNDERSCORE_PATTERN = Pattern.compile("^_+");

    private final NamingConvention delegate;

    public ElasticNamingConvention() {
        this(NamingConvention.snakeCase);
    }

    public ElasticNamingConvention(NamingConvention delegate) {
        this.delegate = delegate;
    }

    @Override
    public String name(String name, Meter.Type type, String baseUnit) {
        return delegate.name(name, type, baseUnit);
    }

    @Override
    public String tagKey(String key) {
        if (key.equals("name")) {
            key = "name.tag";
        } else if (key.equals("type")) {
            key = "type.tag";
        } else if (key.startsWith("_")) {
            // Fields that start with _ are considered reserved and ignored by Kibana. See https://github.com/elastic/kibana/issues/2551
            key = FIRST_UNDERSCORE_PATTERN.matcher(key).replaceFirst("");
        }
        return delegate.tagKey(key);
    }
}