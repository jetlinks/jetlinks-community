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